package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.Car;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Reservation;

@Stateless
public class ManagerSession implements ManagerSessionRemote {
    
    @PersistenceContext
    EntityManager em;
    
    @Override
    public Set<CarType> getCarTypes(String company) {
        List<CarType> carTypeList = em.createQuery(
                "SELECT c.carTypes FROM CarRentalCompany c where c.name = :company")
                        .setParameter("company", company).getResultList();
        return new HashSet<CarType>(carTypeList);
    }

    @Override
    public Set<Integer> getCarIds(String company, String type) {
        Set<Integer> out = new HashSet<Integer>();
        List<Integer> carIds = em.createQuery(
                "select car.id from CarRentalCompany c," + 
                " in (c.cars) car" + 
                " where c.name = :company and car.type.name = :type")
                .setParameter("company", company).setParameter("type", type).getResultList();
        out.addAll(carIds);
        return out;
        
//        try {
//            for(Car c: RentalStore.getRental(company).getCars(type)){
//                out.add(c.getId());
//            }
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        }
//        return out;
    }

    @Override
    public int getNumberOfReservations(String company, String type, int id) {
        // TODO not sure
        int out = em.createQuery(
                "select car.reservations from CarRentalCompany company," + 
                " in (company.cars) car" + 
                " where company.name = :company and car.id = :id")
                .setParameter("company", company).setParameter("id", id)
                .getResultList().size();
        
        return out;
//        try {
//            return RentalStore.getRental(company).getCar(id).getReservations().size();
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
//            return 0;
//        }
    }

    @Override
    public int getNumberOfReservationsByCarType(String company, String type) {
        // TODO not sure
        int sum = 0;
        List<Car> cars = em.createQuery(
                "select car from CarRentalCompany company," + 
                " in (company.cars) car" + 
                " where company.name = :company and car.type.name = :type")
                .setParameter("company", company).setParameter("type", type)
                .getResultList();
        
//        List<List<Reservation>> reservationLists = new LinkedList<>();
        for(Car car : cars){
            sum += car.getReservations().size();
        }
        
//        for(List<Reservation> rs : reservations){
//            if(rs != null){
//                sum += rs.size();
//            }
//        }
        
        return sum;
        
//        Set<Reservation> out = new HashSet<Reservation>();
//        try {
//            for(Car c: RentalStore.getRental(company).getCars(type)){
//                out.addAll(c.getReservations());
//            }
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
//            return 0;
//        }
//        return out.size();
    }

    @Override
    public int getNumberOfReservationsBy(String renter){
        List<Reservation> reservations = em.createQuery(
                "select r from Car car," + 
                " in (car.reservations) r" + 
                " where r.carRenter = :renter").setParameter("renter", renter)
                .getResultList();
//        int numOfRent = 0;
//        for(List<Reservation> rl : reservations){
//            for(Reservation r : rl){
//                if(r.getCarRenter().equals(renter)){
//                    numOfRent++;
//                }
//            }
//        }
        
        return reservations.size();
    }
    
    @Override
    public CarType getMostPopularCarTypeIn(String carRentalCompanyName, int year) {
        List<Car> cars = em.createQuery(
                "select company.cars from CarRentalCompany company" + 
                " where company.name = :company")
                .setParameter("company", carRentalCompanyName)
                .getResultList();
        
        Date firstDayInTheYear = new Date(year, 1, 1);
        Date lastDayInTheYear = new Date(year, 12, 31);
        
        Map<CarType, Integer> numOfReservations = new HashMap<>();
        
        for(Car car : cars){
            for(Reservation res : car.getReservations()){
                if(res.getStartDate().after(firstDayInTheYear) && 
                        res.getStartDate().before(lastDayInTheYear)){
                    if(numOfReservations.containsKey(car.getType())){
                        numOfReservations.put(car.getType(), 
                                numOfReservations.get(car.getType()) + 1);
                    } else {
                        numOfReservations.put(car.getType(), 1);
                    }
                }
                
            }
        }
        
        int maxReservation = 0;
        CarType out = null;
        for(Entry<CarType, Integer> e : numOfReservations.entrySet()){
            if(e.getValue() > maxReservation){
                maxReservation = e.getValue();
                out = e.getKey();
            }
        }
        return out;
    }
    
    @Override
    public Set<String> getBestClients() {
        // TODO not sure
        
        List<Reservation> reservations = em.createQuery(
                "select r from Car car," + 
                " in (car.reservations) r")
                .getResultList();
        
        Map<String, Integer> numOfRent = new HashMap<>();
        
//        for(List<Reservation> rl : reservations){
            for(Reservation r : reservations){
                if(numOfRent.containsKey(r.getCarRenter())){
                    numOfRent.put(r.getCarRenter(), numOfRent.get(r.getCarRenter())+1);
                } else {
                    numOfRent.put(r.getCarRenter(), 1);
                }
            }
//        }
        
        int maxRent = 0;
        for(int n : numOfRent.values()){
            if(n > maxRent){
                maxRent = n;
            }
        }
        
        Set<String> out = new HashSet<>();
        for(Entry<String, Integer> e : numOfRent.entrySet()){
            if(e.getValue() == maxRent){
                out.add(e.getKey());
            }
        }
        
        return out; 
    }
    
//    public void addCarRentalCompany(String name, List<CarType> carTypes, List<String> regions) {
//        List<Car> carsList = new LinkedList<Car>();
//        
//        for (CarType carType : carTypes) {
//            carsList.add(new Car(carType));
//        }
//
//        CarRentalCompany company = new CarRentalCompany(name, regions, carsList);
//        List<Car> carsToCopy = company.popAllCars();
//        manager.persist(company);
//        CarRentalCompany companyEntry = manager.find(CarRentalCompany.class, company.getName());
//        
//        for (Car car : carsToCopy) {
//            CarType type = manager.find(CarType.class, car.getType().toString());
//            if (type != null) {
//                companyEntry.addCarType(type);
//                car.setType(type);
//            }
//            else {
//                companyEntry.addCarType(car.getType());
//                car.setType(car.getType());
//            }
//            companyEntry.addCar(car);
//        }
//    }
    
    @Override
    public void loadRental(String datafile) {
        try {
            CrcData data = loadData(datafile);
            CarRentalCompany company = new CarRentalCompany(data.name, data.regions, data.cars);
//            rentals.put(data.name, company);
            em.persist(company);
            Logger.getLogger(ManagerSession.class.getName()).log(Level.INFO, "Loaded {0} from file {1}", new Object[]{data.name, datafile});
        } catch (NumberFormatException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, "bad file", ex);
        } catch (IOException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private CrcData loadData(String datafile)
            throws NumberFormatException, IOException {

        CrcData out = new CrcData();
        StringTokenizer csvReader;
//        int nextuid = 0;
       
        //open file from jar
        BufferedReader in = new BufferedReader(new InputStreamReader(ManagerSession.class.getClassLoader().getResourceAsStream(datafile)));
        
        try {
            while (in.ready()) {
                String line = in.readLine();
                
                if (line.startsWith("#")) {
                    // comment -> skip					
                } else if (line.startsWith("-")) {
                    csvReader = new StringTokenizer(line.substring(1), ",");
                    out.name = csvReader.nextToken();
                    out.regions = Arrays.asList(csvReader.nextToken().split(":"));
                } else {
                    csvReader = new StringTokenizer(line, ",");
                    //create new car type from first 5 fields
                    CarType type = new CarType(csvReader.nextToken(),
                            Integer.parseInt(csvReader.nextToken()),
                            Float.parseFloat(csvReader.nextToken()),
                            Double.parseDouble(csvReader.nextToken()),
                            Boolean.parseBoolean(csvReader.nextToken()));
                    
                    CarType persistedCarType = em.find(CarType.class, type.getName());
                    
                    if(persistedCarType != null){
                        type = persistedCarType;
                    }else{
                        em.persist(type);
                    }
                    
                    //create N new cars with given type, where N is the 5th field
                    for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
                        Car car = new Car(type);
                        out.cars.add(car);
                        em.persist(car);
//                        out.cars.add(new Car(nextuid++, type));
                    }        
                }
            } 
        } finally {
            in.close();
        }

        return out;
    }
    
    class CrcData {
            public List<Car> cars = new LinkedList<Car>();
            public String name;
            public List<String> regions =  new LinkedList<String>();
    }
    
    
    public void testJPQL(){
        List<Car> li = em.createQuery("select company.cars from CarRentalCompany company").getResultList();
//        List li = 
//                em.createQuery(
//                "select car.reservations from CarRentalCompany company," + 
//                " in (company.cars) car" + 
//                " where company.name = :company and car.id = :id")
//                .setParameter("company", "Hertz").setParameter("id", 1)
//                .getResultList();
                
//                em.createQuery(
//                "select car.reservations from CarRentalCompany company," + 
//                " in (company.cars) car" + 
//                " where company.name = :company and car.type.name = :type")
//                .setParameter("company", "Hertz").setParameter("type", "Compact")
//                .getResultList();
//        
//        System.out.println("size: " + li.size());
        
        for(Car ob : li){
            System.out.println(ob.getId());
        }
    }
    
}