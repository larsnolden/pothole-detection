package edu.utwente.trackingapp;

public class Import {

    private String[] AntennaAMacs = {"D7:08:44:3C:7F:09", "EC:1C:37:B5:B4:4F", "EB:A2:1B:CE:31:EC", "DE:AC:E4:81:12:D6",
            "F9:B0:44:14:19:F9", "D6:12:35:99:CA:17", "C1:6F:31:CF:3E:6D", "EA:64:7A:3A:35:75"
    };

    private String[] AntennaBMacs = {"D5:88:79:6F:9C:44", "CF:3E:A2:5B:33:65", "F7:D6:23:D0:DF:24", "E5:CD:CB:19:D3:17",
            "CB:94:11:47:A9:1F", "D5:19:64:B9:E6:F6", "C8:45:AE:31:6A:C1", "FC:24:C2:3D:D5:2A"};


    private String[] AntennaEMacs = {"D1:7A:C8:8C:21:2D", "F5:FC:EB:45:73:20", "F1:D6:E6:73:C9:B8", "DD:D3:F4:8B:FB:3F",
            "CF:1D:6C:3F:DE:38", "E0:24:97:61:17:5C", "C0:A9:2E:0F:6B:40", "F7:65:2D:7A:F4:A0"};


    public Antena[] getAntennas() {

        Antena antennaA = new Antena();
        Antena antennaB = new Antena();
        Antena antennaE = new Antena();
        Antena beacon1 = new Antena();
        Antena beacon2 = new Antena();
        Antena beacon3 = new Antena();
        Antena beacon4 = new Antena();
        Antena beacon5 = new Antena();
        Antena beacon6 = new Antena();
        Antena beacon7 = new Antena();
        Antena beacon8 = new Antena();
        Antena beacon9 = new Antena();
        Antena beacon10 = new Antena();
        Antena beacon11 = new Antena();
        Antena beacon12 = new Antena();


        antennaA.setMacAddresses(AntennaAMacs);
        antennaA.setName("Antenna A");
        antennaA.setLatitude(52.23927);
        antennaA.setLongitude(6.85649);
        antennaA.setRssi(-1000);
        antennaA.setCorrectionFactor(0.88);

        antennaB.setMacAddresses(AntennaBMacs);
        antennaB.setName("Antenna B");
        antennaB.setLatitude(52.23931);
        antennaB.setLongitude(6.85638);
        antennaB.setRssi(-1000);
        antennaB.setCorrectionFactor(0.86);

        antennaE.setMacAddresses(AntennaEMacs);
        antennaE.setName("Antenna E");
        antennaE.setLatitude(52.23923);
        antennaE.setLongitude(6.85661);
        antennaE.setRssi(-1000);
        antennaE.setCorrectionFactor(1.62);

        beacon1.setMacAddresses(new String[]{"E8:FD:27:B7:8B:D5"});
        beacon1.setName("Beacon 1");
        beacon1.setLatitude(52.23924923475468);
        beacon1.setLongitude(6.856487614735073);
        beacon1.setRssi(-1000);
        beacon1.setCorrectionFactor(1.11);

        beacon2.setMacAddresses(new String[]{"D4:50:EF:62:52:77"});
        beacon2.setName("Beacon 2");
        beacon2.setLatitude(52.23923164860034);
        beacon2.setLongitude(6.856536613352432);
        beacon2.setRssi(-1000);
        beacon2.setCorrectionFactor(1.11);

        beacon3.setMacAddresses(new String[]{"ED:E3:26:AF:5C:FE"});
        beacon3.setName("Beacon 3");
        beacon3.setLatitude(52.239252831704704);
        beacon3.setLongitude(6.856547032335897);
        beacon3.setRssi(-1000);
        beacon3.setCorrectionFactor(2.62);

        beacon4.setMacAddresses(new String[]{"FC:00:55:AC:AE:12"});
        beacon4.setName("Beacon 4");
        beacon4.setLatitude(52.23932544537817);
        beacon4.setLongitude(6.856466420310454);
        beacon4.setRssi(-1000);
        beacon4.setCorrectionFactor(4.11);

        beacon5.setMacAddresses(new String[]{"CA:02:32:D2:A1:83"});
        beacon5.setName("Beacon 5");
        beacon5.setLatitude(52.23936900217345);
        beacon5.setLongitude(6.856266334074746);
        beacon5.setRssi(-1000);
        beacon5.setCorrectionFactor(0.375);

        beacon6.setMacAddresses(new String[]{"E1:11:5E:46:41:31"});
        beacon6.setName("Beacon 6");
        beacon6.setLatitude(52.239349440521636);
        beacon6.setLongitude(6.856418232126771);
        beacon6.setRssi(-1000);
        beacon6.setCorrectionFactor(7.31);

        beacon7.setMacAddresses(new String[]{"D6:CA:74:F4:6F:BB"});
        beacon7.setName("Beacon 7");
        beacon7.setLatitude(52.23929001939251);
        beacon7.setLongitude(6.856307750473803);
        beacon7.setRssi(-1000);
        beacon7.setCorrectionFactor(1.86);

        beacon8.setMacAddresses(new String[]{"E8:9E:C6:63:9A:A6"});
        beacon8.setName("Beacon 8");
        beacon8.setLatitude(52.23926952369595);
        beacon8.setLongitude(6.856603903465157);
        beacon8.setRssi(-1000);
        beacon8.setCorrectionFactor(2.8);

        beacon9.setMacAddresses(new String[]{"CD:79:35:3F:C3:8B"});
        beacon9.setName("Beacon 9");
        beacon9.setLatitude(52.23932626743914);
        beacon9.setLongitude(6.856219971158367);
        beacon9.setRssi(-1000);
        beacon9.setCorrectionFactor(1);

        beacon10.setMacAddresses(new String[]{"DE:AE:F9:8E:93:1D"});
        beacon10.setName("Beacon 10");
        beacon10.setLatitude(52.239365469209076);
        beacon10.setLongitude(6.8563798505130364);
        beacon10.setRssi(-1000);
        beacon10.setCorrectionFactor(5.75);

        beacon11.setMacAddresses(new String[]{"E4:AC:6D:E6:77:7A"});
        beacon11.setName("Beacon 11");
        beacon11.setLatitude(52.239306903322586);
        beacon11.setLongitude(6.856269409105522);
        beacon11.setRssi(-1000);
        beacon11.setCorrectionFactor(2.26);

        beacon12.setMacAddresses(new String[]{"E4:D7:BA:CB:2C:F8"});
        beacon12.setName("Beacon 12");
        beacon12.setLatitude(52.239256522694944);
        beacon12.setLongitude(6.856642705343802);
        beacon12.setRssi(-1000);
        beacon12.setCorrectionFactor(3.5);


        Antena[] antennas = {antennaA, antennaB, antennaE, beacon1, beacon2, beacon3, beacon4, beacon5, beacon6, beacon7, beacon8, beacon9, beacon10, beacon11, beacon12 };

        return antennas;
    }


}
