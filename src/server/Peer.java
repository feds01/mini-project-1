package server;

/**
 * Class to hold metadata on a potential peer
 *
 * @author 200008575
 */
public class Peer {
    /**
     *
     */
    private String address;

    /**
     *
     */
    private String name;

    /**
     *
     */
    public Peer() {
        super();
    }


    /**
     *
     */
    public Peer(String address, String name) {
        this.address = address;
        this.name = name;
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public String getAddress() {
        return address;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return String.format("Peer at %s on %s", address, name);
    }
}
