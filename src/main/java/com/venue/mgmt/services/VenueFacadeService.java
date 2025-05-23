package com.venue.mgmt.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.venue.mgmt.controller.VenueController;
import com.venue.mgmt.entities.Venue;
import com.venue.mgmt.repositories.LeadRegRepository;
import com.venue.mgmt.repositories.VenueRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.venue.mgmt.constant.GeneralMsgConstants.*;

@Service
public class VenueFacadeService {

    private final GooglePlacesService googleMapsService;

    private final VenueRepository venueRepos;

    private final VenueService venueService;

    private static final Logger logger = LogManager.getLogger(VenueFacadeService.class);

    public VenueFacadeService(GooglePlacesService googleMapsService, VenueRepository venueRepos,VenueService venueService) {
        this.googleMapsService = googleMapsService;
        this.venueRepos = venueRepos;
        this.venueService=venueService;
    }

    public void fetchAndSetAddressDetails(Venue venue) throws Exception {
        JsonNode geocodeResponse = googleMapsService.geocodeAddress(venue.getAddress());
        if (geocodeResponse == null || !geocodeResponse.has("results") || geocodeResponse.path("results").isEmpty()) {
            return;
        }
        JsonNode addressComponents = geocodeResponse.path("results").get(0).path("address_components");
        for (JsonNode component : addressComponents) {
            List<String> types = new ArrayList<>();
            component.path("types").forEach(type -> types.add(type.asText()));
            if (types.contains(POSTAL_CODE)) {
                venue.setPinCode(component.path(LONG_NAME).asText());
            }
            if (types.contains(LOCALITY)) {
                venue.setCity(component.path(LONG_NAME).asText());
            }
            if( types.contains(SUB_LOCALITY)) {
                venue.setLocality(component.path(LONG_NAME).asText());
            }
            if (types.contains(STATE)) {
                venue.setState(component.path(LONG_NAME).asText());
            }
            if (types.contains(COUNTRY)) {
                venue.setCountry(component.path(LONG_NAME).asText());
            }
        }
    }

    public void calculateTotalLeadsCount(List<Venue> venues, String userId, LeadRegRepository leadRegRepository) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date today = calendar.getTime();
        for (Venue venue : venues) {
            int leadCount = leadRegRepository.countByVenue_VenueIdAndCreatedByAndIsDeletedFalse(
                    venue.getVenueId(), userId);
            // Count today's leads
            int leadCountToday = leadRegRepository.countByVenue_VenueIdAndCreatedByAndCreationDateAndIsDeletedFalse(
                    venue.getVenueId(), userId, today);
            venue.setLeadCount(leadCount);
            venue.setLeadCountToday(leadCountToday);
        }
    }

    public Page<Venue> getVenuesByLocationOrDefault(String location, String userId, Pageable pageable) {
        Double lat = null;
        Double lon = null;

        // Parse location if provided
        if (location != null && location.contains(",")) {
            try {
                String[] parts = location.split(",");
                lat = Double.parseDouble(parts[0].trim());
                lon = Double.parseDouble(parts[1].trim());
            } catch (Exception e) {
                logger.warn("Invalid location format. Expected 'lat,lon'. Got: " + location);
            }
        }

        if (lat != null && lon != null) {
            final double finalLat = lat;
            final double finalLon = lon;
            logger.info("Latitude and Longitude provided — applying KNN logic");
            List<Venue> allVenues = venueRepos.findAll();

            logger.info("Total venues fetched: {}", allVenues.size());

            // Sort venues by Haversine distance
            allVenues.forEach(venue -> {
                double distance = venueService.calculateDistance(finalLat, finalLon, venue.getLatitude(), venue.getLongitude());
                venue.setDistance(distance); // store for sorting
                logger.info("Venue ID: {}, Distance: {}", venue.getVenueId(), distance);
            });
            allVenues.sort(Comparator.comparingDouble(Venue::getDistance));

            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allVenues.size());
            logger.info("Pagination - Start: {}, End: {}", start, end);
            List<Venue> pagedList = allVenues.subList(start, end);

            return new PageImpl<>(pagedList, pageable, allVenues.size());
        } else {
            // Default sorting by creationDate
            return venueService.getAllVenuesSortedByCreationDate(
                    pageable.getSort().toString(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    userId
            );
        }
    }
}
