package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
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
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.Car;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Reservation;

@DeclareRoles("Manager")
@RolesAllowed("Manager")
@Stateless
public class ManagerSession implements ManagerSessionRemote {
    private String name;
    
    @PersistenceContext
    EntityManager em;
    
    public void setName(String name){
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
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

    }

    @Override
    public int getNumberOfReservationsByCarType(String company, String type) {
        int sum = 0;
        List<Car> cars = em.createQuery(
                "select car from CarRentalCompany company," + 
                " in (company.cars) car" + 
                " where company.name = :company and car.type.name = :type")
                .setParameter("company", company).setParameter("type", type)
                .getResultList();
        
        for(Car car : cars){
            sum += car.getReservations().size();
        }
        
        return sum;
        
    }

    @Override
    public int getNumberOfReservationsBy(String renter){
        List<Reservation> reservations = em.createQuery(
                "select r from Car car," + 
                " in (car.reservations) r" + 
                " where r.carRenter = :renter")
                .setParameter("renter", renter)
                .getResultList();

        return reservations.size();
    }
    
    
    
    // TODO : Improve the following two methods to use more JPQL
    @Override
    public CarType getMostPopularCarTypeIn(String carRentalCompanyName, int year) {
        List<Car> cars = em.createQuery(
                "select company.cars from CarRentalCompany company" + 
                " where company.name = :company")
                .setParameter("company", carRentalCompanyName)
                .getResultList();
        
        Map<CarType, Integer> numOfReservations = new HashMap<>();
        
        for(Car car : cars){
            for(Reservation res : car.getReservations()){
                if(res.getStartDate().getYear() == year - 1900){
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
        
        List<Reservation> reservations = em.createQuery(
                "select r from Car car," + 
                " in (car.reservations) r")
                .getResultList();
        
        Map<String, Integer> numOfRent = new HashMap<>();

        for(Reservation r : reservations){
            if(numOfRent.containsKey(r.getCarRenter())){
                numOfRent.put(r.getCarRenter(), numOfRent.get(r.getCarRenter())+1);
            } else {
                numOfRent.put(r.getCarRenter(), 1);
            }
        }
        
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
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void loadRental(String datafile) {
        try {
            CrcData data = loadData(datafile);
            CarRentalCompany company = new CarRentalCompany(data.name, data.regions, data.cars);
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
                    
                    CarType persistedCarType = em.find(CarType.class, type.getId());
                    
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
    
}