import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Flight implements Comparable<Flight> {

	//times are implemented as an integer representing the amount of minutes from the beginning of the week: Monday midnight.
	//A single day is 1440 minutes
	//there are 10080 minutes total in the whole week
	private int departure;
	
	//comparator to sort flights by arrival time
	//if equal arrival times, then sort on destination name
	public static final Comparator<Flight> compareByArrival = new Comparator<Flight>() {
		@Override
		public int compare(Flight f1, Flight f2) {
			if (f1.getArrivalTime() - f2.getArrivalTime() < 0) {
				return -1;
			}
			if (f1.getArrivalTime() == f2.getArrivalTime()) {
				return f1.getTo().getName().compareTo(f2.getTo().getName());
			}
			return 1;
		}
	};
	
	private int id;	
	private Location from;
	private Location to;
	private int capacity;
	private int booked;
	//private double ticketPrice;
	
	public Flight(int departure, Location from, Location to, int capacity) {
		this.departure = departure;
		this.from = from;
		this.to = to;
		this.capacity = capacity;
		
		
		this.booked = 0;
		
	}
	
	public int getDepartureTime() { return this.departure; }
	public int getId() { return this.id; }
	public Location getFrom() { return this.from; }
	public Location getTo() { return this.to; }
	public int getCapacity() { return this.capacity; }
	public int getBooked() { return this.booked; }
	
	public void assignId() {
		this.id = FlightScheduler.getInstance().getFlightCount();
		FlightScheduler.getInstance().incrementFlightCount();
	}
	
	public void setDepartureTime(int d) {
		this.departure = d;
	}
	
	public double book(int num) {
		if (num <= 0) return 0;
		int bookNumber = num;
		if (this.booked + num > this.capacity) {
			bookNumber = this.capacity - this.booked;
		}
		double totalCost = 0;
		for (int i = 0; i < bookNumber; i++) {
			totalCost += getTicketPrice();
			this.booked += 1;
		}
		return totalCost;
	}
	
	public void reset() {
		this.booked = 0;
	}
	
	public boolean isFull() {
		return getCapacity()==getBooked();
	}
	
	public double getTicketPrice() {
		double proportionBooked = getBooked() / ((double) getCapacity());
		double multiplier = 0;
		if (proportionBooked <= 0.5) {
			multiplier = 1 - 0.4*proportionBooked;
		} else if (proportionBooked <= 0.7) {
			multiplier = proportionBooked+0.3;
		} else {
			multiplier = 0.2 * Math.atan(20*proportionBooked - 14)/Math.PI + 1;
		}
		double distance = this.getDistance();
		double coeffDifferential = to.getDemandCoefficient()-from.getDemandCoefficient();
		return multiplier*(30 + 4*coeffDifferential)*(distance/100);	
	}
	
	public int getDuration() {
		return (int)Math.round(getDistance()*60.0 / 720.0);
	}
	
	public int getArrivalTime() {
		return (this.getDepartureTime() + this.getDuration()) % 10080;
	}
	
	public double getDistance() {
		return Location.distance(from, to);
	}
	
	//default sorting is by departure time, then departure location name
	@Override
	public int compareTo(Flight l2) {
		if (this.getDepartureTime() - l2.getDepartureTime() < 0) {
			return -1;
		}
		if (this.getDepartureTime() == l2.getDepartureTime()) {
			return this.getFrom().getName().compareTo(l2.getFrom().getName());
		}
		return 1;
	}
	
	
	// Flight Path static methods
	public static List<Comparator<Flight[]>> pathComparator = new ArrayList<Comparator<Flight[]>>();
	
	static {
		//cheapest always first
		pathComparator.add(new Comparator<Flight[]>() {
			@Override
			public int compare(Flight[] path1, Flight[] path2) {
				double path1Cost = totalCost(path1);
				double path2Cost = totalCost(path2);
				int x = Double.compare(path1Cost, path2Cost);
				if (x == 0) {
					return Double.compare(totalDuration(path1), totalDuration(path2));
				}
				return x;
			}
		});
		
		//min total duration, then cheapest
		pathComparator.add(new Comparator<Flight[]>() {
			@Override
			public int compare(Flight[] path1, Flight[] path2) {
				int path1Duration = totalDuration(path1);
				int path2Duration = totalDuration(path2);
				int x = Integer.compare(path1Duration, path2Duration);
				if (x == 0) {
					return pathComparator.get(0).compare(path1, path2);
				}
				return x;
			}
		});
		
		//min stopovers, then min duration, then cheapest
		pathComparator.add(new Comparator<Flight[]>() {
			@Override
			public int compare(Flight[] path1, Flight[] path2) {
				int x = Integer.compare(path1.length, path2.length);
				if (x == 0) {
					return pathComparator.get(1).compare(path1, path2);
				}
				return x;
			}
		});
		
		//min layover time, then min duration, then cheapest
		pathComparator.add(new Comparator<Flight[]>() {
			@Override
			public int compare(Flight[] path1, Flight[] path2) {
				int path1Cost = totalLayover(path1);
				int path2Cost = totalLayover(path2);
				int x = Integer.compare(path1Cost, path2Cost);
				if (x == 0) {
					return pathComparator.get(1).compare(path1, path2);
				}
				return x;
			}
		});
		
		//min flight time, then min duration, then cheapest
		pathComparator.add(new Comparator<Flight[]>() {
			@Override
			public int compare(Flight[] path1, Flight[] path2) {
				int path1Cost = totalFlightTime(path1);
				int path2Cost = totalFlightTime(path2);
				int x = Integer.compare(path1Cost, path2Cost);
				if (x == 0) {
					return pathComparator.get(1).compare(path1, path2);
				}
				return x;
			}
		});
	}
	
	public static double totalCost(Flight[] path) {
		double result = 0;
		for (int i = 0; i < path.length; i++) {
			result += path[i].getTicketPrice();
		}
		return result;
	}
	
	public static int totalDuration(Flight[] path) {
		if (path.length <= 0) return 0;
		int result = 0;
		for (int i = 1; i < path.length; i++) {
			Flight x = path[i-1];
			Flight y = path[i];
			int arrival = x.getArrivalTime();
			int departure = y.getDepartureTime();
			if (arrival < departure) {
				result += x.getDuration() + departure-arrival;
			} else {
				result += x.getDuration() + departure-arrival + 10080;
			}
		}
		return result+path[path.length-1].getDuration();
	}
	
	public static int totalLayover(Flight[] path) {
		int result = 0;
		for (int i = 1; i < path.length; i++) {
			Flight x = path[i-1];
			Flight y = path[i];
			result += layover(x,y);
		}
		return result;
	}
	
	public static int layover(Flight x, Flight y) {
		int arrival = x.getArrivalTime();
		int departure = y.getDepartureTime();
		if (arrival < departure) {
			return departure-arrival;
		} else {
			return departure-arrival + 10080;
		}
	}
	
	public static int totalFlightTime(Flight[] path) {
		int flightTime = 0;
		for (int i = 0; i < path.length; i++) {
			flightTime += path[i].getDuration();
		}
		return flightTime;
	}
}
