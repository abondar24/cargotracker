package org.eclipse.cargotracker.domain.model.voyage;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.cargotracker.domain.model.location.Location;

/**
 * A carrier movement is a vessel voyage from one location to another.
 */
@Entity
@Table(name = "carrier_movement")
public class CarrierMovement implements Serializable {

	private static final long serialVersionUID = 1L;

	// Null object pattern
	public static final CarrierMovement NONE = new CarrierMovement(Location.UNKNOWN, Location.UNKNOWN, new Date(0),
			new Date(0));

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	@JoinColumn(name = "departure_location_id")
	@NotNull
	private Location departureLocation;

	@ManyToOne
	@JoinColumn(name = "arrival_location_id")
	@NotNull
	private Location arrivalLocation;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "departure_time")
	@NotNull
	private Date departureTime;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "arrival_time")
	@NotNull
	private Date arrivalTime;

	public CarrierMovement() {
		// Nothing to initialize.
	}

	public CarrierMovement(Location departureLocation, Location arrivalLocation, Date departureTime, Date arrivalTime) {
		Validate.noNullElements(new Object[] { departureLocation, arrivalLocation, departureTime, arrivalTime });
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.departureLocation = departureLocation;
		this.arrivalLocation = arrivalLocation;
	}

	public Location getDepartureLocation() {
		return departureLocation;
	}

	public Location getArrivalLocation() {
		return arrivalLocation;
	}

	public Date getDepartureTime() {
		return new Date(departureTime.getTime());
	}

	public Date getArrivalTime() {
		return new Date(arrivalTime.getTime());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		CarrierMovement that = (CarrierMovement) o;

		return sameValueAs(that);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.departureLocation).append(this.departureTime)
				.append(this.arrivalLocation).append(this.arrivalTime).toHashCode();
	}

	private boolean sameValueAs(CarrierMovement other) {
		return other != null && new EqualsBuilder().append(this.departureLocation, other.departureLocation)
				.append(this.departureTime, other.departureTime).append(this.arrivalLocation, other.arrivalLocation)
				.append(this.arrivalTime, other.arrivalTime).isEquals();
	}
}
