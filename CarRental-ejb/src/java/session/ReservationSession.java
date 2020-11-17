package session;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PersistenceContext;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Quote;
import rental.Reservation;
import rental.ReservationConstraints;
import rental.ReservationException;

//@NamedQueries({
//    
////    @NamedQuery(
////            name = "getAllCompanies",
////            query = "select c.name from CarRentalCompany c"
////    ),
//    
//    @NamedQuery(
//            name = "getACompany",
//            query = "select c from CarRentalCompany c" + 
//                " where c.name = :company"
//    ),
//    
//})

@Stateful
public class ReservationSession implements ReservationSessionRemote {

    private String renter;
    private List<Quote> quotes = new LinkedList<Quote>();

    @PersistenceContext
    EntityManager em;
    
    @Override
    public Set<String> getAllRentalCompanies() {
        List<String> tmp = em.createQuery(
                "select c.name from CarRentalCompany c").getResultList();
        Set<String> out = new HashSet<String>(tmp);
        return out;
//        return new HashSet<String>(RentalStore.getRentals().keySet());
    }
    
    @Override
    public List<CarType> getAvailableCarTypes(Date start, Date end) {
        List<CarRentalCompany> companies = em.createQuery(
                "select c from CarRentalCompany c").getResultList();
        List<CarType> availableCarTypes = new LinkedList<CarType>();
        Set<CarType> tmp = new HashSet<CarType>();
        for(CarRentalCompany company : companies){
            tmp.addAll(company.getAvailableCarTypes(start, end));
        }
//        for(String crc : getAllRentalCompanies()) {
//            for(CarType ct : RentalStore.getRentals().get(crc).getAvailableCarTypes(start, end)) {
//                if(!availableCarTypes.contains(ct))
//                    availableCarTypes.add(ct);
//            }
//        }
        return new LinkedList<CarType>(tmp);
    }
    
    @Override
    public String getCheapestCarType(Date start, Date end, String region) {
        List<CarRentalCompany> companies = em.createQuery(
                "select c from CarRentalCompany c").getResultList();
        List<CarType> allCarTypes = new LinkedList<>();
        for( CarRentalCompany company : companies){
            if(company.getRegions().contains(region)){
                allCarTypes.addAll(company.getAvailableCarTypes(start, end));
            }
        }
        double minPrice = allCarTypes.get(0).getRentalPricePerDay();
        String out = allCarTypes.get(0).getName();
        for(CarType ct : allCarTypes){
            System.out.println("price: "+ minPrice);
            System.out.println("type: "+ out);
            double price = ct.getRentalPricePerDay();
            if(price < minPrice){
                minPrice = price;
                
                out = ct.getName();
                
            }
        }
        return out;
    }

    @Override
    public Quote createQuote(ReservationConstraints constraints) throws ReservationException {
        List<CarRentalCompany> crcs = em.createQuery(
                "select c from CarRentalCompany c")
                .getResultList();
        
        int numOfExceptions = 0;
        Quote out = null;
        for(CarRentalCompany crc : crcs){
            try {
                out = crc.createQuote(constraints, renter);
                quotes.add(out);
            } catch(Exception e) {
                numOfExceptions++;
                continue;
            }
        }
        
        if(numOfExceptions == crcs.size()){
            throw new ReservationException("No company is avaliable for this reservation");
        }
        
        return out;
        
        
//        try {
//            Quote out = RentalStore.getRental(company).createQuote(constraints, renter);
//            quotes.add(out);
//            return out;
//        } catch(Exception e) {
//            throw new ReservationException(e);
//        }
    }

    @Override
    public List<Quote> getCurrentQuotes() {
        return quotes;
    }

    @Override
    public List<Reservation> confirmQuotes() throws ReservationException {
        List<Reservation> done = new LinkedList<Reservation>();
        
        
        
        try {
            for (Quote quote : quotes) {
                CarRentalCompany crc = (CarRentalCompany)em.createQuery(
                "select c from CarRentalCompany c" + 
                " where c.name = :company")
                .setParameter("company", quote.getRentalCompany())
                .getResultList().get(0);
                done.add(crc.confirmQuote(quote));
//                done.add(RentalStore.getRental(quote.getRentalCompany()).confirmQuote(quote));
            }
        } catch (Exception e) {
            for(Reservation r:done) {
                CarRentalCompany crc = (CarRentalCompany)em.createQuery(
                "select c from CarRentalCompany c" + 
                " where c.name = :company")
                .setParameter("company", r.getRentalCompany())
                .getResultList().get(0);
                
                crc.cancelReservation(r);
            }
                
//                RentalStore.getRental(r.getRentalCompany()).cancelReservation(r);
            throw new ReservationException(e);
        }
        return done;
    }

    @Override
    public void setRenterName(String name) {
        if (renter != null) {
            throw new IllegalStateException("name already set");
        }
        renter = name;
    }

    @Override
    public String getRenterName() {
        return renter;
    }
    
    public void testJPQL() {
        CarRentalCompany crc = (CarRentalCompany)em.createQuery(
                "select c from CarRentalCompany c" + 
                " where c.name = :company")
                .setParameter("company", "Hertz")
                .getResultList().get(0);
        System.out.println(crc.getName());
    }
}