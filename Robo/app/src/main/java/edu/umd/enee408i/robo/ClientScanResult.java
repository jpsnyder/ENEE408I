package edu.umd.enee408i.robo;

public class ClientScanResult {

    private String IpAddr;

    private String HWAddr;

    private String Device;

    private boolean isReachable;

    public ClientScanResult(String ipAddr, String hWAddr, String device, boolean isReachable) {
        super();
        IpAddr = ipAddr;
        HWAddr = hWAddr;
        Device = device;
        this.isReachable = isReachable;
    }

    public String getIpAddr() {
        return IpAddr;
    }

    public void setIpAddr(String ipAddr) {
        IpAddr = ipAddr;
    }

    public String getHWAddr() {
        return HWAddr;
    }

    public void setHWAddr(String hWAddr) {
        HWAddr = hWAddr;
    }
}