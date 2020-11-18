package session;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Quote;
import rental.Reservation;
import rental.ReservationConstraints;
import rental.ReservationException;


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

        return new LinkedList<CarType>(tmp);
    }
    
    @Override
    public String getCheapestCarType(Date start, Date end, String region) {
        List<CarRentalCompany> companies = em.createQuery(
                "select c from CarRentalCompany c," + 
                " in (c.regions) r" + 
                " where r = :region")
                .setParameter("region", region)
                .getResultList();
        
        List<CarType> allCarTypes = new LinkedList<>();

        for( CarRentalCompany company : companies){
            allCarTypes.addAll(company.getAvailableCarTypes(start, end));
        }
        
        double minPrice = Double.MAX_VALUE;
        String out = null;
        for(CarType ct : allCarTypes){
            double price = ct.getRentalPricePerDay();
            if(price < minPrice){
                minPrice = price;
                
                out = ct.getName();
                
            }
        }
        return out;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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
        
    }

    @Override
    public List<Quote> getCurrentQuotes() {
        return quotes;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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

}