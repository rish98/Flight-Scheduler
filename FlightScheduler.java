import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class FlightScheduler {
	
	private int flightCount = 0;
	private HashMap<Integer, Flight> flights = new HashMap<Integer, Flight>();
	private Flight latest = null;
	private String message = null;
	
	private HashMap<String, Location> locations = new HashMap<String, Location>();
	private Location latestLoc = null;
	private static FlightScheduler instance;

	public static void main(String[] args) {
		instance = new FlightScheduler(args);
		instance.run();
	}
	
	public static FlightScheduler getInstance() {
		return instance;
	}

	public FlightScheduler(String[] args) {}
	
	public int getFlightCount() {
		return this.flightCount;
	}
	
	public void incrementFlightCount() {
		this.flightCount++;
	}

	public void run() {
		/*File[] filesList = new File(".").listFiles();
		for(File f : filesList){
			System.out.println(f.getName());
		}*/
		Scanner scan = new Scanner(System.in);
		
		System.out.print("User: ");

		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equalsIgnoreCase("exit")) {
				System.out.println("Application closed.");
				break;
			}
			
			String[] parts = line.split(" ");	
			if (parts[0].equalsIgnoreCase("flights")) {
				System.out.println("Flights");
				System.out.println("-------------------------------------------------------");
				System.out.println("ID   Departure   Arrival     Source --> Destination");
				System.out.println("-------------------------------------------------------");
				//Example:
				//System.out.println("123  Mon 00:00   Mon 12:56   Sydney --> Los Angeles");
				List<Flight> sortedList = new ArrayList<Flight>(flights.values());
				Collections.sort(sortedList/*, new Comparator<Flight>() {
					@Override
					public int compare(Flight l1, Flight l2) {
						if (l1.getDepartureTime() - l2.getDepartureTime() < 0) {
							return -1;
						}
						if (l1.getDepartureTime() == l2.getDepartureTime()) {
							return l1.getFrom().getName().compareTo(l2.getFrom().getName());
						}
						return 1;
					}
				}*/);
				for (Flight in : sortedList) {
					System.out.printf("%4d %s   %s   %s --> %s\n", 
						in.getId(), 
						fromTimestamp(in.getDepartureTime()), 
						fromTimestamp(in.getArrivalTime()), 
						in.getFrom().getName(),
						in.getTo().getName());
				}
				if (sortedList.size() == 0) {
					System.out.println("(None)");
				}
			} else if (parts[0].equalsIgnoreCase("flight")) {
				if (parts.length > 1) {
					if (parts[1].equalsIgnoreCase("import")) {
						importFlights(parts);
					} else if (parts[1].equalsIgnoreCase("export")) {
						exportFlights(parts);
					} else if (parts[1].equalsIgnoreCase("add")) {
						if (parts.length < 7) {
							System.out.println("Usage:   FLIGHT ADD <departure time> <from> <to> <capacity>");
							System.out.println("Example: FLIGHT ADD Monday 18:00 Sydney Melbourne 120");
						} else {
														
							int status = addFlight(parts[2], parts[3], parts[4], parts[5], parts[6], 0);
							if (status == -1) System.out.println("Invalid departure time. Use the format <day_of_week> <hour:minute>, with 24h time.");
							else if (status == -2) System.out.println("Invalid starting location.");
							else if (status == -3) System.out.println("Invalid ending location.");
							else if (status == -4) System.out.println("Invalid positive integer capacity.");
							else if (status == -5) System.out.println("Source and destination cannot be the same place.");
							else if (status == -6) System.out.println("Scheduling conflict! This flight clashes with "+message);
							else if (status == 0) System.out.println("Successfully added Flight "+latest.getId()+".");
							else System.out.println("Status code "+status+".");
						}
					} else {
						try {
							int f = Integer.parseInt(parts[1]);
							Flight found = flights.get(f);
							if (found == null) throw new NumberFormatException();
							
							if (parts.length > 2) {
								if (parts[2].equalsIgnoreCase("book")) {
									int numberBooked = 1;
									if (parts.length > 3) {
									try {
										numberBooked = Integer.parseInt(parts[3]);
									} catch (NumberFormatException e2) {
										System.out.println("Invalid number of passengers to book.");
										System.out.print("\nUser: ");
										continue;
									}}
									int bookedBefore = found.getBooked();
									double bookingCost = found.book(numberBooked);
									int bookedAfter = found.getBooked();
									
									System.out.printf("Booked "+ String.valueOf(bookedAfter-bookedBefore) +" passengers on flight "+found.getId()+" for a total cost of $%.2f\n", bookingCost);
									if (found.isFull()) {
										System.out.println("Flight is now full.");
									}
								} else if (parts[2].equalsIgnoreCase("remove")) {
									flights.remove(f);
									found.getFrom().departing.remove(found);
									found.getTo().arriving.remove(found);
									System.out.println("Removed Flight "+found.getId()+", "+fromTimestamp(found.getDepartureTime())+" "+found.getFrom().getName()+" --> "+found.getTo().getName()+", from the flight schedule.");
								} else if (parts[2].equalsIgnoreCase("reset")) {
									found.reset();
									System.out.println("Reset passengers booked to 0 for Flight "+found.getId()+", "+fromTimestamp(found.getDepartureTime())+" "+found.getFrom().getName()+" --> "+found.getTo().getName()+".");
								
								} 
							} else {
								//display info
								/*
								Flight 1
								Departure:    Mon 00:00 Sydney
								Arrival:      Mon 12:56 Los Angeles
								Distance:     1,435km
								Duration:     12h 56min
								Ticket Cost:  $685.32
								Passengers:   86/120
								*/
								int dira = found.getDuration();
								System.out.printf("Flight %d\nDeparture:    %s\nArrival:      %s\nDistance:     %,dkm\nDuration:     %dh %dm\nTicket Cost:  $%.2f\nPassengers:   %d/%d\n",
									found.getId(),
									fromTimestamp(found.getDepartureTime())+" "+found.getFrom().getName(),
									fromTimestamp(found.getArrivalTime())+" "+found.getTo().getName(),
									(int)Math.round(found.getDistance()),
									dira/60, dira%60,
									found.getTicketPrice(),
									found.getBooked(), found.getCapacity());
								
							}
							
						} catch (NumberFormatException e) {
							System.out.println("Invalid Flight ID.");
						}
					}
				} else {
					System.out.println("Usage:");
					System.out.println("FLIGHT <id> [BOOK/REMOVE/RESET] [num]");
					System.out.println("FLIGHT ADD <departure time> <from> <to> <capacity>");
					System.out.println("FLIGHT IMPORT/EXPORT <filename>");
				}
			} else if (parts[0].equalsIgnoreCase("locations")) {
				System.out.println("Locations ("+locations.size()+"):");
				//System.out.println("Sydney, Melbourne, Hobart, Perth, Adelaide, Brisbane, Orange, Dubbo, Coffs Harbour");
				List<Location> sortedList = new ArrayList<Location>(locations.values());
				Collections.sort(sortedList, new Comparator<Location>() {
					@Override
					public int compare(Location l1, Location l2) {
						return l1.getName().compareTo(l2.getName());
					}
				});
				if (sortedList.size() == 0) {
					System.out.println("(None)");
				} else {
				
					StringBuilder sb = new StringBuilder(sortedList.get(0).getName());
					for (int i = 1; i < sortedList.size(); i++) {
						sb.append(", "+sortedList.get(i).getName());
					}
					System.out.println(sb.toString());	
				}
			} else if (parts[0].equalsIgnoreCase("location")) {
				if (parts.length > 1) {
					if (parts[1].equalsIgnoreCase("import")) {
						importLocations(parts);
					} else if (parts[1].equalsIgnoreCase("export")) {
						exportLocations(parts);
					} else if (parts[1].equalsIgnoreCase("add")) {
						if (parts.length < 6) {
							System.out.println("Usage:   LOCATION ADD <name> <lat> <long> <demand_coefficient>");
							System.out.println("Example: LOCATION ADD Sydney -33.847927 150.651786 0.2");
						} else {
														
							int status = addLocation(parts[2], parts[3], parts[4], parts[5]);
							if (status == -1) System.out.println("This location already exists.");
							else if (status == -2) System.out.println("Invalid latitude. It must be a number of degrees between -85 and +85.");
							else if (status == -3) System.out.println("Invalid longitude. It must be a number of degrees between -180 and +180.");
							else if (status == -4) System.out.println("Invalid demand coefficient. It must be a number between -1 and +1.");
							else if (status == 0) System.out.println("Successfully added location "+latestLoc.getName()+".");
							else System.out.println("Status code "+status+".");
						}
					} else {
						Location found = locations.get(parts[1].toLowerCase());
						if (found == null) {
							System.out.println("Invalid location name.");
						} else {
								//display info
								/*
								Location:    Sydney
								Latitude:    -33.847927
								Longitude:   150.651786
								Demand:      +0.2
								*/
								System.out.printf("Location:    %s\nLatitude:    %.6f\nLongitude:   %.6f\nDemand:      %+.4f\n",
									found.getName(),
									found.getLatitude(),
									found.getLongitude(), 
									found.getDemandCoefficient());
							
						}
					}
				} else {
					System.out.println("Usage:");
					System.out.println("LOCATION <name>");
					System.out.println("LOCATION ADD <name> <latitude> <longitude> <demand_coefficient>");
					System.out.println("LOCATION IMPORT/EXPORT <filename>");
				}
			} else if (parts[0].equalsIgnoreCase("schedule")) {
				if (parts.length > 1) {
					Location loc = locations.get(parts[1].toLowerCase());
					if (loc == null) {
						System.out.println("This location does not exist in the system.");
					} else {
						System.out.println(loc.getName());
						System.out.println("-------------------------------------------------------");
						System.out.println("ID   Time        Departure/Arrival to/from Location");
						System.out.println("-------------------------------------------------------");
						TreeMap<Integer, String> schedule = new TreeMap<Integer, String>();
						
						for (Flight in : loc.departing) {
							schedule.put(in.getDepartureTime(), 
									String.format("%4d %s   %s\n", 
											in.getId(), 
											fromTimestamp(in.getDepartureTime()), 
											"Departure to "+in.getTo().getName()));
						}
						for (Flight in : loc.arriving) {
							int arrivalTime = in.getArrivalTime();
							schedule.put(arrivalTime, 
									String.format("%4d %s   %s\n", 
											in.getId(), 
											fromTimestamp(arrivalTime),
											"Arrival from "+in.getFrom().getName()));
						}
						
						for (Map.Entry<Integer, String> in : schedule.entrySet()) {
							System.out.print(in.getValue());
						}
					}
				} else {
					System.out.println("This location does not exist in the system.");
				}
			} else if (parts[0].equalsIgnoreCase("departures")) {
				if (parts.length > 1) {
					Location loc = locations.get(parts[1].toLowerCase());
					if (loc == null) {
						System.out.println("This location does not exist in the system.");
					} else {
						System.out.println(loc.getName());
						System.out.println("-------------------------------------------------------");
						System.out.println("ID   Time        Departure/Arrival to/from Location");
						System.out.println("-------------------------------------------------------");
						for (Flight in : loc.departing) {
							System.out.printf("%4d %s   %s\n", 
									in.getId(), 
									fromTimestamp(in.getDepartureTime()), 
									"Departure to "+in.getTo().getName());
						}
					}
				} else {
					System.out.println("This location does not exist in the system.");
				}
			} else if (parts[0].equalsIgnoreCase("arrivals")) {
				if (parts.length > 1) {
					Location loc = locations.get(parts[1].toLowerCase());
					if (loc == null) {
						System.out.println("This location does not exist in the system.");
					} else {
						System.out.println(loc.getName());
						System.out.println("-------------------------------------------------------");
						System.out.println("ID   Time        Departure/Arrival to/from Location");
						System.out.println("-------------------------------------------------------");
						for (Flight in : loc.arriving) {
							System.out.printf("%4d %s   %s\n", 
									in.getId(), 
									fromTimestamp(in.getArrivalTime()), 
									"Arrival from "+in.getFrom().getName());
						}
					}
				} else {
					System.out.println("This location does not exist in the system.");
				}
			} else if (parts[0].equalsIgnoreCase("travel")) {
				
				if (parts.length > 2) {
					int status = getFlights(parts[1].toLowerCase(), parts[2].toLowerCase(), parts.length>3?parts[3]:null, parts.length>4?parts[4]:null);
					
					if (status == -1) System.out.println("Starting location not found.");
					else if (status == -2) System.out.println("Ending location not found.");
					else if (status == -3) System.out.println("Invalid sorting property: must be either cost, duration, stopovers, layover, or flight_time.");
					
				} else {
					System.out.println("Usage: TRAVEL <from> <to> [cost/duration/stopovers/layover/flight_time]");
				}
			} else if (parts[0].equalsIgnoreCase("help")) {
				
				System.out.println("FLIGHTS - list all available flights ordered by departure time, then departure location name\n"+
"FLIGHT ADD <departure time> <from> <to> <capacity> - add a flight\n"+
"FLIGHT IMPORT/EXPORT <filename> - import/export flights to csv file\n"+
"FLIGHT <id> - view information about a flight (from->to, departure arrival times, current ticket price, capacity, passengers booked)\n"+
"FLIGHT <id> BOOK <num> - book a certain number of passengers for the flight at the current ticket price, and then adjust the ticket price to reflect the reduced capacity remaining. If no number is given, book 1 passenger. If the given number of bookings is more than the remaining capacity, only accept bookings until the capacity is full.\n"+
"FLIGHT <id> REMOVE - remove a flight from the schedule\n"+
"FLIGHT <id> RESET - reset the number of passengers booked to 0, and the ticket price to its original state.\n"+
"\n"+
"LOCATIONS - list all available locations in alphabetical order\n"+
"LOCATION ADD <name> <lat> <long> <demand_coefficient> - add a location\n"+
"LOCATION <name> - view details about a location (it's name, coordinates, demand coefficient)\n"+
"LOCATION IMPORT/EXPORT <filename> - import/export locations to csv file\n"+
"SCHEDULE <location_name> - list all departing and arriving flights, in order of the time they arrive/depart\n"+
"DEPARTURES <location_name> - list all departing flights, in order of departure time\n"+
"ARRIVALS <location_name> - list all arriving flights, in order of arrival time\n"+
"\n"+
"TRAVEL <from> <to> [sort] [n] - list the nth possible flight route between a starting location and destination, with a maximum of 3 stopovers. Default ordering is for shortest overall duration. If n is not provided, display the first one in the order. If n is larger than the number of flights available, display the last one in the ordering.\n"+
"\n"+
"can have other orderings:\n"+
"TRAVEL <from> <to> cost - minimum current cost\n"+
"TRAVEL <from> <to> duration - minimum total duration\n"+
"TRAVEL <from> <to> stopovers - minimum stopovers\n"+
"TRAVEL <from> <to> layover - minimum layover time\n"+
"TRAVEL <from> <to> flight_time - minimum flight time\n"+
"\n"+
"HELP - outputs this help string.\n"+
"EXIT - end the program.");
				
			} else {
				System.out.println("Invalid command. Type 'help' for a list of commands.");
			}
			System.out.print("\nUser: ");
			
		}
		scan.close();
	}

	
	
	private int getFlights(String f, String t, String s, String sortedOrderNum) {
		Location from = locations.get(f);
		if (from == null) return -1;
		Location to = locations.get(t);
		if (to == null) return -2;
		
		int sortedOrderNumber = 0;
		try {
			if (sortedOrderNum != null) {
				sortedOrderNumber = Integer.parseInt(sortedOrderNum);
				if (sortedOrderNumber < 0) sortedOrderNumber = 0;
			}
		} catch (NumberFormatException e) {}
		
		int sort = -1;
		
		if (s == null) sort = 1;
		else if (s.equalsIgnoreCase("cost")) sort = 0; //cheapest always first
		else if (s.equalsIgnoreCase("duration")) sort = 1; //min total duration, then cheapest
		else if (s.equalsIgnoreCase("stopovers")) sort = 2; //min stopovers, then min duration, then cheapest
		else if (s.equalsIgnoreCase("layover")) sort = 3; //min layover time, then min duration, then cheapest
		else if (s.equalsIgnoreCase("flight_time")) sort = 4; //min flight time, then min duration, then cheapest
		else return -3;
		
		List<Flight[]> noStopovers = new ArrayList<Flight[]>();
		List<Flight[]> oneStopover = new ArrayList<Flight[]>();
		List<Flight[]> twoStopovers = new ArrayList<Flight[]>();
		List<Flight[]> threeStopovers = new ArrayList<Flight[]>();
		
		for (Flight f1 : from.departing) {
			//if (f1.isFull()) continue;
			Location s1 = f1.getTo();
			if (s1.equals(to)) {
				noStopovers.add(new Flight[]{f1});
			} else {
				for (Flight f2 : s1.departing) {
					//if (f2.isFull()) continue;
					Location s2 = f2.getTo();
					if (s2.equals(to)) {
						oneStopover.add(new Flight[]{f1, f2});
					} else {
						for (Flight f3 : s2.departing) {
							//if (f3.isFull()) continue;
							Location s3 = f3.getTo();
							if (s3.equals(to)) {
								twoStopovers.add(new Flight[]{f1,f2,f3});
							} else {
								for (Flight f4 : s3.departing) {
									//if (f4.isFull()) continue;
									if (f4.getTo().equals(to)) {
										threeStopovers.add(new Flight[] {f1,f2,f3,f4});
									}
								}
							}
						}
					}
				}
			}
		}
		
		List<Flight[]> paths = new ArrayList<Flight[]>();
		paths.addAll(noStopovers);
		paths.addAll(oneStopover);
		paths.addAll(twoStopovers);
		paths.addAll(threeStopovers);
		
		Collections.sort(paths, Flight.pathComparator.get(sort));
		
		if (paths.size() == 0) {
			System.out.println("Sorry, no flights with 3 or less stopovers are available from "+from.getName()+" to "+to.getName()+".");
			return 0;
		}
		
		if (sortedOrderNumber >= paths.size()) {
			sortedOrderNumber = paths.size()-1;
		}
		
		Flight[] selected = paths.get(sortedOrderNumber);
		int dura = Flight.totalDuration(selected);
		System.out.printf("Legs:             %d\n",selected.length);
		System.out.printf("Total Duration:   %dh %dm\n",dura/60, dura%60);
		System.out.printf("Total Cost:       $%.2f\n",Flight.totalCost(selected));
		System.out.println("-------------------------------------------------------------");
		System.out.println("ID   Cost      Departure   Arrival     Source --> Destination");
		System.out.println("-------------------------------------------------------------");
		
		for (int i = 0; i < selected.length; i++) {
			Flight in = selected[i];
			if (i != 0) {
				int layoverTime = Flight.layover(selected[i-1], in);
				System.out.printf("LAYOVER %dh %dm at %s\n", layoverTime/60, layoverTime%60, in.getFrom().getName());
			}
			
			System.out.printf("%4d $%8.2f %s   %s   %s --> %s\n", 
					in.getId(), 
					in.getTicketPrice(),
					fromTimestamp(in.getDepartureTime()), 
					fromTimestamp(in.getArrivalTime()), 
					in.getFrom().getName(),
					in.getTo().getName());
			
		}
		
		return 0;
	}

	public static String[] days = new String[] {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	public static String[] days2 = new String[] {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
	
	//return the departure time corresponding to this day, hour and minute
	public static int toTimestamp(String day, int hour, int minute) {
		if (day == null || hour < 0 || minute < 0) return -1;
		day = day.toLowerCase();
		int d = -1;
		if (day.equals("monday")) d = 0;
		else if (day.equals("tuesday")) d = 1;
		else if (day.equals("wednesday")) d = 2;
		else if (day.equals("thursday")) d = 3;
		else if (day.equals("friday")) d = 4;
		else if (day.equals("saturday")) d = 5;
		else if (day.equals("sunday")) d = 6;
		else return -1;
		
		return d*1440 + hour*60 + minute;
	}
	
	//return the String datetime corresponding to this departure time int
	public static String fromTimestamp(int t) {
		int d = t/1440;
		if (d > days.length) return null;
		String day = FlightScheduler.days[d];
		int hour = (t % 1440) / 60;
		int minute = t % 60;
		return day + " "+(hour<10?"0":"")+hour+":"+(minute<10?"0":"")+minute;
	}
	
	public static String fromTimestamp2(int t) {
		int d = t/1440;
		if (d > days2.length) return null;
		String day = FlightScheduler.days2[d];
		int hour = (t % 1440) / 60;
		int minute = t % 60;
		return day + " "+(hour<10?"0":"")+hour+":"+(minute<10?"0":"")+minute;
	}
	
	public void importFlights(String[] command) {
		try {
			if (command.length < 3) throw new FileNotFoundException();
			BufferedReader br = new BufferedReader(new FileReader(new File(command[2])));
			String line;
			int count = 0;
			int err = 0;
			
			while ((line = br.readLine()) != null) {
				String[] lparts = line.split(",");
				if (lparts.length < 5) continue;
				String[] dparts = lparts[0].split(" ");
				if (dparts.length < 2) continue;
				int booked = 0;
				
				try {
					booked = Integer.parseInt(lparts[4]);
					
				} catch (NumberFormatException e) {
					continue;
				}
				
				int status = addFlight(dparts[0], dparts[1], lparts[1], lparts[2], lparts[3], booked);
				if (status < 0) {
					err++;
					continue;
				}
				count++;
			}
			br.close();
			System.out.println("Imported "+count+" flight"+(count!=1?"s":"")+".");
			if (err > 0) {
				if (err == 1) System.out.println("1 line was invalid.");
				else System.out.println(err+" lines were invalid.");
			}
		} catch (IOException e) {
			System.out.println("Error reading file.");
			return;
		}
	}
	
	public int addFlight(String date1, String date2, String s, String e, String c, int b) {
		int depart = -1;
		try {
			String[] dpparts = date2.split(":");
			if (dpparts.length < 2) return -1;
			depart = toTimestamp(date1, Integer.parseInt(dpparts[0]), Integer.parseInt(dpparts[1]));
			if (depart < 0) return -1;
		} catch (NumberFormatException ex) {
			return -1;
		}
		
		Location start = locations.get(s.toLowerCase());
		if (start == null) {
			return -2;
		}
		Location end = locations.get(e.toLowerCase());
		if (end == null) {
			return -3;
		}
		
		try {
			int cap = Integer.parseInt(c);
			if (cap < 0) {
				return -4;
			}
			if (start.equals(end)) return -5;
			Flight nf = new Flight(depart, start, end, cap);
			
			String clash1 = start.hasRunwayDepartureSpace(nf);
			if (clash1 != null) {
				message = clash1;
				return -6;
			}
			
			String clash2 = end.hasRunwayArrivalSpace(nf);
			if (clash2 != null) {
				message = clash2;
				return -6;
			}
			
			start.addDeparture(nf);
			end.addArrival(nf);
			
			nf.assignId();
			flights.put(nf.getId(), nf);
			nf.book(b);
			latest = nf;
		} catch (NumberFormatException ex) {
			return -4;
		}
		return 0;
	}
	
	public void exportFlights(String[] command) {
		try {
			if (command.length < 3) throw new FileNotFoundException();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(command[2])));
			List<Flight> fl = new ArrayList<Flight>(flights.values());
			Collections.sort(fl, new Comparator<Flight>() {
				@Override
				public int compare(Flight f1, Flight f2) {
					return Integer.compare(f1.getId(), f2.getId());
				}
			});
			for (Flight in : fl) {
				bw.write(fromTimestamp2(in.getDepartureTime())+","+
					in.getFrom().getName().replace(",", "")+","+
					in.getTo().getName().replace(",", "")+","+
					in.getCapacity()+","+
					in.getBooked());
				bw.newLine();
			}
			bw.close();
			System.out.println("Exported "+flights.size()+" flight"+(flights.size()!=1?"s":"")+".");
			
		} catch (IOException e) {
			System.out.println("Error writing file.");
			return;
		}
	}
	
	public int addLocation(String name, String lat, String lon, String dem) {
		if (locations.containsKey(name.toLowerCase())) {
			return -1;
		}
		double latitude = 0;
		try {
			latitude = Double.parseDouble(lat);
			if (latitude > 85 || latitude < -85) return -2;
		} catch (NumberFormatException e) {
			return -2;
		}
		
		double longitude = 0;
		try {
			longitude = Double.parseDouble(lon);
			if (longitude > 180 || longitude < -180) return -3;
		} catch (NumberFormatException e) {
			return -3;
		}
		
		double demand = 0;
		try {
			demand = Double.parseDouble(dem);
			if (demand < -1 || demand > 1) return -4;
		} catch (NumberFormatException e) {
			return -4;
		}
		
		Location nl = new Location(name, latitude, longitude, demand);
		locations.put(name.toLowerCase(), nl);
		latestLoc = nl;
		return 0;
		
	}
	
	public void importLocations(String[] command) {
		try {
			if (command.length < 3) throw new FileNotFoundException();
			BufferedReader br = new BufferedReader(new FileReader(new File(command[2])));
			String line;
			int count = 0;
			int err = 0;
			
			while ((line = br.readLine()) != null) {
				String[] lparts = line.split(",");
				if (lparts.length < 4) continue;
								
				int status = addLocation(lparts[0], lparts[1], lparts[2], lparts[3]);
				if (status < 0) {
					err++;
					continue;
				}
				count++;
			}
			br.close();
			System.out.println("Imported "+count+" location"+(count!=1?"s":"")+".");
			if (err > 0) {
				if (err == 1) System.out.println("1 line was invalid.");
				else System.out.println(err+" lines were invalid.");
			}
			
		} catch (IOException e) {
			System.out.println("Error reading file.");
			return;
		}
	}
	
	public void exportLocations(String[] command) {
		try {
			if (command.length < 3) throw new FileNotFoundException();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(command[2])));
			List<Location> ll = new ArrayList<Location>(locations.values());
			Collections.sort(ll, new Comparator<Location>() {
				@Override
				public int compare(Location l1, Location l2) {
					return l1.getName().compareTo(l2.getName());
				}
			});
			for (Location in : ll) {
				bw.write(in.getName().replace(",", "")+","+
					in.getLatitude()+","+
					in.getLongitude()+","+
					in.getDemandCoefficient());
				bw.newLine();
			}
			bw.close();
			System.out.println("Exported "+locations.size()+" location"+(locations.size()!=1?"s":"")+".");
			
		} catch (IOException e) {
			System.out.println("Error writing file.");
			return;
		}
	}
	
	public static boolean withinAnHour(int t1, int t2) {
		if (Math.abs(t1-t2) < 60) return true;
		if (Math.abs(t1-t2) > 10020) return true;
		return false;
	}
	
}
