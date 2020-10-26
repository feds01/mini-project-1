package server;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
     * Variable to represent if this peer entry references the current instance
     * of the application. We don't need to transmit this value to other peers
     * because other peers will all also return that they are their own connection.
     * This value is used for internal representation.
     */
    @JsonIgnore
    private boolean isSelf = false;

    /**
     * Variable to represent if the connection was observed to be
     * dead or alive,
     */
    boolean isAlive = true;

    /**
     * Default constructor that can be used by Jackson for data de-serialization
     */
    public Peer() {
        super();
    }


    /**
     *
     */
    public Peer(String address, String name, boolean isAlive) {
        this.address = address;
        this.name = name;
        this.isAlive = isAlive;
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
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
        var sb = new StringBuilder();

        sb.append(String.format("(%s) Peer at %s on %s ", isAlive ? "RUNNING" : "DEAD",  address, name));

        // Add a label of 'self' to denote that this is our own connection.
        if (this.isSelf) {
            sb.append("(self)");
        }

        return sb.toString();
    }
}
