package growup.spring.springserver.databaseutils.testdatagenerator;

import java.util.List;
import java.util.Random;

public class RandomOptionNameGenerator {
    static final List<String> optionsNames = List.of("red","black","pink","orange","green","blue");

    public static String getRandomOptionNames(){
        Random random = new Random();
        return optionsNames.get((int) (random.nextLong(optionsNames.size()))) + random.nextInt(100);
    }

}
