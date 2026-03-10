package growup.spring.springserver.databaseutils.testdatagenerator;

public enum DataFormat {
    USERNAME("user"),
    EMAIL_FORMAT("@test.com");

    private final String value;
    DataFormat(String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }

    public static String convertNumberToUserName(String number){
        return USERNAME.getValue() + number;
    }
    public static String convertNumberToUserName(int number){
        String stringTypeNumber = String.valueOf(number);
        if(number < 10){
            stringTypeNumber = "0"+ stringTypeNumber;
        }
        return USERNAME.getValue()+stringTypeNumber;
    }
    public static String convertNumberToUserEmail(String number){
        return convertNumberToUserName(number) + EMAIL_FORMAT.getValue();
    }
    public static String convertNumberToUserEmail(int number){
        return convertNumberToUserName(number)+EMAIL_FORMAT.getValue();
    }
}
