package com.venue.mgmt.services.impl;

import com.venue.mgmt.dto.LeadPatchDTO;
import com.venue.mgmt.entities.Campaign;
import com.venue.mgmt.entities.LeadRegistration;
import com.venue.mgmt.repositories.CampaignRepository;
import com.venue.mgmt.repositories.LeadRegRepository;
import com.venue.mgmt.services.LeadRegistrationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class LeadRegistrationServiceImpl implements LeadRegistrationService {

    private static final Logger logger = LogManager.getLogger(LeadRegistrationServiceImpl.class);
    
    @Autowired
    private LeadRegRepository leadRegRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public LeadRegistration saveLead(LeadRegistration leadRegistration) {
        try {
            logger.info("Starting to save lead with campaign value: {}", leadRegistration.getCampaign());
            
            // Handle campaign creation from the request
            String campaignValue = leadRegistration.getCampaign();
            if (campaignValue != null && !campaignValue.trim().isEmpty()) {
                Campaign campaign = new Campaign();
                campaign.setCampaignName(campaignValue);
                
                // Save the campaign first
                campaign = campaignRepository.save(campaign);

                // Now add it to the lead
                leadRegistration.addCampaign(campaign);
            }

            // Save the lead registration
            logger.info("Saving lead registration...");
            LeadRegistration savedLead = leadRegRepository.save(leadRegistration);

            return savedLead;
        } catch (Exception e) {
            logger.error("Error while saving lead with campaign: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadRegistration> getAllLeadsSortedByCreationDate(String sortDirection, int page, int size) {
        try {
            Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, "creationDate");
            Pageable pageable = PageRequest.of(page, size, sort);
            return leadRegRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("Error while fetching all leads: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadRegistration> simpleSearchLeads(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllLeadsSortedByCreationDate("desc", 0, Integer.MAX_VALUE).getContent();
            }
            return leadRegRepository.searchLeads(searchTerm);
        } catch (Exception e) {
            logger.error("Error while searching leads: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LeadRegistration updateLead(Long leadId, LeadRegistration updatedLead) {
        try {
            LeadRegistration existingLead = leadRegRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with id: " + leadId));

            // Update the fields
            existingLead.setEmail(updatedLead.getEmail());
            existingLead.setMobileNumber(updatedLead.getMobileNumber());
            existingLead.setStatus(updatedLead.getStatus());

            // Handle campaign update if provided
            String campaignValue = updatedLead.getCampaign();
            if (campaignValue != null && !campaignValue.trim().isEmpty()) {
                // Remove existing campaign if any
                if (existingLead.getCampaignEntity() != null) {
                    existingLead.removeCampaign();
                }
                
                // Add new campaign
                Campaign newCampaign = new Campaign();
                newCampaign.setCampaignName(campaignValue);
                existingLead.addCampaign(newCampaign);
            }

            LeadRegistration savedLead = leadRegRepository.save(existingLead);
            logger.info("Updated lead with ID: {}", savedLead.getLeadId());
            return savedLead;
        } catch (Exception e) {
            logger.error("Error while updating lead: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteLead(Long leadId) {
        try {
            LeadRegistration lead = leadRegRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with id: " + leadId));
            
            // Remove associated campaign if exists
            if (lead.getCampaignEntity() != null) {
                lead.removeCampaign();
            }
            
            leadRegRepository.delete(lead);
            logger.info("Deleted lead with ID: {}", leadId);
        } catch (Exception e) {
            logger.error("Error while deleting lead: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LeadRegistration patchLead(Long leadId, LeadPatchDTO leadPatchDTO) {
        try {
            LeadRegistration existingLead = leadRegRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with id: " + leadId));

            // Update only the fields that are present in the patch request
            if (leadPatchDTO.hasField("name")) {
                existingLead.setFullName(leadPatchDTO.getName());
            }
            if (leadPatchDTO.hasField("email")) {
                existingLead.setEmail(leadPatchDTO.getEmail());
            }
            if (leadPatchDTO.hasField("mobileNumber")) {
                existingLead.setMobileNumber(leadPatchDTO.getMobileNumber());
            }
            if (leadPatchDTO.hasField("status")) {
                existingLead.setStatus(leadPatchDTO.getStatus());
            }

            // Handle campaign update if provided
            if (leadPatchDTO.hasField("campaign")) {
                String campaignValue = leadPatchDTO.getCampaign();
                if (campaignValue != null && !campaignValue.trim().isEmpty()) {
                    // Remove existing campaign if any
                    if (existingLead.getCampaignEntity() != null) {
                        existingLead.removeCampaign();
                    }
                    
                    // Add new campaign
                    Campaign newCampaign = new Campaign();
                    newCampaign.setCampaignName(campaignValue);
                    existingLead.addCampaign(newCampaign);
                }
            }

            LeadRegistration savedLead = leadRegRepository.save(existingLead);
            logger.info("Patched lead with ID: {}", savedLead.getLeadId());
            return savedLead;
        } catch (Exception e) {
            logger.error("Error while patching lead: {}", e.getMessage(), e);
            throw e;
        }
    }
}
