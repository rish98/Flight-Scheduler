import java.util.TreeSet;

public class Location {
	
	private String name;
	private double lat;
	private double lon;
	private double demand;
	
	//a TreeSet is an ordered collection. It allows more efficient retrieval with O(logn) operations
	//you can just as easily use a List, which for the operations required would take O(n)
	// - requires iterating over all the list elements
	public TreeSet<Flight> arriving;
	public TreeSet<Flight> departing;

	public Location(String name, double lat, double lon, double demand) {
		this.name = name;
		this.lat = lat;
		this.lon = lon;
		//sort arriving flights by arrival time
		arriving = new TreeSet<Flight>(Flight.compareByArrival);
		//sort departing flights by departure time (default)
		departing = new TreeSet<Flight>();
		setDemandCoefficient(demand);
	}
	
	public String getName() { return this.name; }
	public double getLatitude() { return this.lat; }
	public double getLongitude() { return this.lon; }
	public double getDemandCoefficient() { return this.demand; }
	
	public boolean setDemandCoefficient(double d) {
		if (d < -1 || d > 1) return false;
		this.demand = d;
		return true;
	}
	
	public void addArrival(Flight f) {
		this.arriving.add(f);
	}
	
	public void addDeparture(Flight f) {
		this.departing.add(f);
	}
	
	/**
	 * Check to see if Flight f can depart from this location.
	 * If there is a clash, the clashing flight string is returned, otherwise null is returned.
	 * A conflict is determined by if any other flights are arriving or departing at this location within an hour of this flight's departure time.
	 * @param f The flight to check.
	 * @return "Flight <id> [departing/arriving] from <name> on <clashingFlightTime>". Return null if there is no clash.
	 */
	public String hasRunwayDepartureSpace(Flight f) {
		Flight after2 = departing.ceiling(f);
		Flight before2 = departing.floor(f);
		//is there a conflicting flight also departing within an hour of this one?
		if (after2 != null || before2 != null) {
			if (after2 == null) {
				after2 = departing.first();
			}
			if (after2 != null && FlightScheduler.withinAnHour(after2.getDepartureTime(),f.getDepartureTime())) {
				return "Flight "+after2.getId()+" departing from "+this.getName()+" on "+FlightScheduler.fromTimestamp2(after2.getDepartureTime())+".";
			}
			if (before2 == null) {
				before2 = departing.last();
			}
			if (before2 != null && FlightScheduler.withinAnHour(f.getDepartureTime(),before2.getDepartureTime())) {
				return "Flight "+before2.getId()+" departing from "+this.getName()+" on "+FlightScheduler.fromTimestamp2(before2.getDepartureTime())+".";
			}
		}
		
		//correction to make this flight's arrival time it's departure time,
		//because the TreeSet is sorted by arrival time
		int dura = f.getDuration();
		int oldDeparture = f.getDepartureTime();
		int ndeparture = f.getDepartureTime()-dura;
		if (ndeparture < 0) ndeparture += 10080;
		f.setDepartureTime(ndeparture);
		Flight after = arriving.ceiling(f);
		Flight before = arriving.floor(f);
		//is there a conflicting flight also arriving within an hour this flight's departure time?
		f.setDepartureTime(oldDeparture);
		if (after != null || before != null) {
			if (after == null) {
				after = arriving.first();
			}
			if (after != null && FlightScheduler.withinAnHour(after.getArrivalTime(), oldDeparture)) {
				return "Flight "+after.getId()+" arriving at "+this.getName()+" on "+FlightScheduler.fromTimestamp2(after.getArrivalTime())+".";
			}
			if (before == null) {
				before = arriving.last();
			}
			if (before != null && FlightScheduler.withinAnHour(oldDeparture ,before.getArrivalTime())) {
				
				return "Flight "+before.getId()+" arriving at "+this.getName()+" on "+FlightScheduler.fromTimestamp2(before.getArrivalTime())+".";
			}
		}
		return null;
	}

    /**
	 * Check to see if Flight f can arrive at this location.
	 * A conflict is determined by if any other flights are arriving or departing at this location within an hour of this flight's arrival time.
	 * @param f The flight to check.
	 * @return String representing the clashing flight, or null if there is no clash. Eg. "Flight <id> [departing/arriving] from <name> on <clashingFlightTime>"
	 */
	public String hasRunwayArrivalSpace(Flight f) {
		//correction to make this flight's departure time it's arrival time,
		//because the TreeSet is sorted by departures time
		int oldDeparture = f.getDepartureTime();
		int ndeparture = f.getArrivalTime();
		f.setDepartureTime(ndeparture);
		
		Flight after2 = departing.ceiling(f);
		Flight before2 = departing.floor(f);
		f.setDepartureTime(oldDeparture);
		//is there a conflicting flight also departing within an hour of this one's arrival?
		if (after2 != null || before2 != null) {
			if (after2 == null) {
				after2 = departing.first();
			}
			if (after2 != null && FlightScheduler.withinAnHour(after2.getDepartureTime(),ndeparture)) {
				return "Flight "+after2.getId()+" departing from "+this.getName()+" on "+FlightScheduler.fromTimestamp2(after2.getDepartureTime())+".";
			}
			if (before2 == null) {
				before2 = departing.last();
			}
			if (before2 != null && FlightScheduler.withinAnHour(ndeparture,before2.getDepartureTime())) {
				return "Flight "+before2.getId()+" departing from "+this.getName()+" on "+FlightScheduler.fromTimestamp2(before2.getDepartureTime())+".";
			}
		}
		
		
		Flight after = arriving.ceiling(f);
		Flight before = arriving.floor(f);
		//is there a conflicting flight also arriving within an hour this flight's arrival time?
		if (after != null || before != null) {
			if (after == null) {
				after = arriving.first();
			}
			if (after != null && FlightScheduler.withinAnHour(after.getArrivalTime(), f.getArrivalTime())) {
				return "Flight "+after.getId()+" arriving at "+this.getName()+" on "+FlightScheduler.fromTimestamp2(after.getArrivalTime())+".";
			}
			if (before == null) {
				before = arriving.last();
			}
			if (before != null && FlightScheduler.withinAnHour(f.getArrivalTime(),before.getArrivalTime())) {
				return "Flight "+before.getId()+" arriving at "+this.getName()+" on "+FlightScheduler.fromTimestamp2(before.getArrivalTime())+".";
			}
		}
		
		return null;
	}

	public static double distance(Location l1, Location l2) {
		double phi1 = l1.getLatitude() * Math.PI/180;
		double phi2 = l2.getLatitude() * Math.PI/180;
		double phidiff = phi2-phi1;
		double lambdadiff = (l2.getLongitude()-l1.getLongitude()) * Math.PI/180;
		double a = Math.pow(Math.sin(phidiff/2),2)+Math.cos(phi1)*Math.cos(phi2)*Math.pow(Math.sin(lambdadiff/2),2);
		return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	}
}
