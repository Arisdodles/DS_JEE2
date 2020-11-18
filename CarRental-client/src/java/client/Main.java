package client;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.InitialContext;
import rental.CarType;
import rental.Reservation;
import rental.ReservationConstraints;
import session.ManagerSessionRemote;
import session.ReservationSessionRemote;

public class Main extends AbstractTestManagement<ReservationSessionRemote, ManagerSessionRemote> {
    Map<String, ManagerSessionRemote> managerSessions = new HashMap<>();
    
    public Main(String scriptFile) {
        super(scriptFile);
    }
    
    public static void main(String[] args) throws Exception {
        
        Main client = new Main("trips");
        
        ManagerSessionRemote initManagerSession = client.getNewManagerSession("init");

        initManagerSession.loadRental("hertz.csv");
        initManagerSession.loadRental("dockx.csv");

        client.run();
    }

    @Override
    protected Set<String> getBestClients(ManagerSessionRemote ms) throws Exception {
        return ms.getBestClients();
    }

    @Override
    protected String getCheapestCarType(ReservationSessionRemote session, Date start, Date end, String region) throws Exception {
        return session.getCheapestCarType(start, end, region);
    }

    @Override
    protected CarType getMostPopularCarTypeIn(ManagerSessionRemote ms, String carRentalCompanyName, int year) throws Exception {
        return ms.getMostPopularCarTypeIn(carRentalCompanyName, year);
    }

    @Override
    protected ReservationSessionRemote getNewReservationSession(String name) throws Exception {
        InitialContext context = new InitialContext();
        ReservationSessionRemote session = (ReservationSessionRemote) context.lookup(ReservationSessionRemote.class.getName());
        session.setRenterName(name);
        sessions.put(name, session);
        return session;
    }

    @Override
    protected ManagerSessionRemote getNewManagerSession(String name) throws Exception {
        InitialContext context = new InitialContext();
        ManagerSessionRemote session = (ManagerSessionRemote) context.lookup(ManagerSessionRemote.class.getName());
        
        managerSessions.put(name, session);
        return session;
    }

    @Override
    protected void getAvailableCarTypes(ReservationSessionRemote session, Date start, Date end) throws Exception {
        session.getAvailableCarTypes(start, end);
    }

    @Override
    protected void createQuote(ReservationSessionRemote session, String name, Date start, Date end, String carType, String region) throws Exception {
        ReservationConstraints constraints = new ReservationConstraints(start, end, carType, region);
        session.createQuote(constraints);
    }

    @Override
    protected List<Reservation> confirmQuotes(ReservationSessionRemote session, String name) throws Exception {
        return session.confirmQuotes();
    }

    @Override
    protected int getNumberOfReservationsBy(ManagerSessionRemote ms, String clientName) throws Exception {
        return ms.getNumberOfReservationsBy(clientName);
    }

    @Override
    protected int getNumberOfReservationsByCarType(ManagerSessionRemote ms, String carRentalName, String carType) throws Exception {
        return ms.getNumberOfReservationsByCarType(carRentalName, carType);
    }
    
}