package server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

/**
 * Class to hold metadata on a potential peer
 *
 * @author 200008575
 */
public class PeerRecord {
    /**
     * The remote address of the peer connection
     */
    private String address;

    /**
     * The host name of the peer connection
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
    public PeerRecord() {
        super();
    }


    /**
     *
     */
    public PeerRecord(String address, String name, boolean isAlive) {
        this.address = address;
        this.name = name;
        this.isAlive = isAlive;
    }

    /**
     * Method to set whether or not this is a record of our own connection.
     * This method should be ignored when using Jackson serialization since
     * if we serialized this value, all peers would return that they are 'self'
     * connections.
     *
     * @param isSelf whether or not this connection is a reference of the current
     *               program peer.
     * */
    @JsonIgnore
    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    /**
     * Method to set whether this peer record connection is dead or alive.
     * Since peer records aren't removed from the 'knownPeer' list even if
     * it times out, it is important to distinguish between connections that
     * are dead or alive. It is also important to store this value since when
     * other peers receive information about peers, they should also receive
     * whether or not the 'knownPeers' are dead or alive.
     *
     * @param isAlive - Whether or not the connection is dead or alive.
     * */
    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }


    /**
     * Method to get whether this 'Peer' connection is actually
     * referencing our own address
     *
     * @return boolean whether this connection is this peer.
     */
    @JsonIgnore
    public boolean isSelf() {
        return isSelf;
    }

    /**
     * Method to get the host name of this peer connection record.
     *
     * @return the host name
     */
    public String getName() {
        return name;
    }

    /**
     * Method to get the address of this peer connection record.
     */
    public String getAddress() {
        return address;
    }

    /**
     * This method is used to print a string representing this peer string. The
     * default method is overridden since the record needs to be readable when
     * accessed by the user in the CLI.
     *
     * @return the string representation of the peer record.
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerRecord peer = (PeerRecord) o;
        return isSelf == peer.isSelf &&
                isAlive == peer.isAlive &&
                Objects.equals(address, peer.address) &&
                Objects.equals(name, peer.name);
    }
}
