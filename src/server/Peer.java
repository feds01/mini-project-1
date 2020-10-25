package server;

public class Peer {
    private String address;
    private String name;

    public Peer() {
        super();
    }

    public Peer(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address='" + address + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
