package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
    public int getNumberOfReservations(String company, String type) {
        // TODO not sure
        int sum = 0;
        List<List<Reservation>> reservations = em.createQuery(
                "select car.reservations from CarRentalCompany company," + 
                " in (company.cars) car" + 
                " where company.name = :company and car.type.name = :type")
                .setParameter("company", company).setParameter("type", type)
                .getResultList();
        
        for(List<Reservation> rs : reservations){
            if(rs != null){
                sum += rs.size();
            }
        }
        
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

    
    public Set<String> getBestClients() {
        // TODO not sure
        
        long maxRent = (Long)em.createQuery(
                "select max(num_rent) from " + 
                " (select count(*) as num_rent from Reservation r" + 
                " group by r.carRenter)")
                .getResultList().get(0);
        
        List<String> renters = em.createQuery(
                "select renter from" +
                " (select r.carRenter as renter, count(r.id) as num_rent from Reservation r" + 
                " group by r.carRenter" + 
                " order by count(r.id) desc)" + 
                " where num_rent = :maxRent").setParameter("maxRent", maxRent)
                .getResultList();
        
        return new HashSet<String>(renters); 
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
        List<List> li = em.createQuery("select car.id, car.type.name from Car car").getResultList();
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
        
        for(List ob : li){
            System.out.println(ob.get(0)+" "+ob.get(1));
        }
    }
    
}