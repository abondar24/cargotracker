package org.eclipse.cargotracker.application;


import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.cargotracker.domain.model.cargo.*;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Application layer integration test covering a number of otherwise fairly
 * trivial components that largely do not warrant their own tests.
 * <p>
 * Ensure a Payara instance is running locally before this test is executed,
 * with the default user name and password.
 */
//TODO [Jakarta EE 8] Move to the Java Date-Time API for date manipulation. Also avoid hard-coded dates.
@Testcontainers
@Tag("integration")
public class BookingServiceTest  {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingServiceTest.class);

    private static TrackingId trackingId;
    private static List<Itinerary> candidates;
    private static Date deadline;
    private static Itinerary assigned;
    @Inject
    private BookingService bookingService;
    @PersistenceContext
    private EntityManager entityManager;

    private static final MountableFile warFile = MountableFile.forHostPath(
            Paths.get("target/cargo-tracker.war").toAbsolutePath(), 511);


    @Container
    private static GenericContainer<?> cargoContainer = new GenericContainer<>("payara/server-full")
            .withCopyFileToContainer(warFile, "/opt/payara/deployments/cargo-tracker.war")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/cargo-tracker"))
            .withStartupTimeout(Duration.ofSeconds(200))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));



    @Test
    public void testRegisterNew() {
        UnLocode fromUnlocode = new UnLocode("USCHI");
        UnLocode toUnlocode = new UnLocode("SESTO");

        deadline = new Date();
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(deadline);
        calendar.add(Calendar.MONTH, 6); // Six months ahead.
        deadline.setTime(calendar.getTime().getTime());

        trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode, deadline);

        Cargo cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
        assertEquals(SampleLocations.STOCKHOLM, cargo.getRouteSpecification().getDestination());
        assertTrue(DateUtils.isSameDay(deadline, cargo.getRouteSpecification().getArrivalDeadline()));
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
        assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.NOT_ROUTED, cargo.getDelivery().getRoutingStatus());
        assertEquals(Itinerary.EMPTY_ITINERARY, cargo.getItinerary());
    }

    @Test
    public void testRouteCandidates() {
        candidates = bookingService.requestPossibleRoutesForCargo(trackingId);

        assertFalse(candidates.isEmpty());
    }

    @Test
    public void testAssignRoute() {
        assigned = candidates.get(new Random().nextInt(candidates.size()));

        bookingService.assignCargoToRoute(assigned, trackingId);

        Cargo cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(assigned, cargo.getItinerary());
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertTrue(cargo.getDelivery().getEstimatedTimeOfArrival().before(deadline));
        assertEquals(HandlingEvent.Type.RECEIVE, cargo.getDelivery().getNextExpectedActivity().getType());
        assertEquals(SampleLocations.CHICAGO, cargo.getDelivery().getNextExpectedActivity().getLocation());
        assertNull(cargo.getDelivery().getNextExpectedActivity().getVoyage());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.ROUTED, cargo.getDelivery().getRoutingStatus());
    }

    @Test
    public void testChangeDestination() {
        bookingService.changeDestination(trackingId, new UnLocode("FIHEL"));

        Cargo cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
        assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification().getDestination());
        assertTrue(DateUtils.isSameDay(deadline, cargo.getRouteSpecification().getArrivalDeadline()));
        assertEquals(assigned, cargo.getItinerary());
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
        assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());
    }

    @Test
    public void testChangeDeadline() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(deadline);
        cal.add(Calendar.MONTH, 1); // Change the deadline one month ahead of the original
        Date newDeadline = cal.getTime();
        bookingService.changeDeadline(trackingId, newDeadline);

        Cargo cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
        assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification().getDestination());
        assertTrue(DateUtils.isSameDay(newDeadline, cargo.getRouteSpecification().getArrivalDeadline()));
        assertEquals(assigned, cargo.getItinerary());
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
        assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());
    }

    @AfterAll
    static void shutdown(){
        cargoContainer.stop();
    }
}
