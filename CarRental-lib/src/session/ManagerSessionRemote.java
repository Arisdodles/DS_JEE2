package session;

import java.util.Set;
import javax.ejb.Remote;
import rental.CarType;
import rental.Reservation;

@Remote
public interface ManagerSessionRemote {
    
    public Set<CarType> getCarTypes(String company);
    
    public Set<Integer> getCarIds(String company,String type);
    
    public int getNumberOfReservations(String company, String type, int carId);
    
    public int getNumberOfReservationsByCarType(String company, String type);

    public void loadRental(String datafile);

    Set<String> getBestClients();

    CarType getMostPopularCarTypeIn(String carRentalCompanyName, int year);

    int getNumberOfReservationsBy(String renter);
    
    
}