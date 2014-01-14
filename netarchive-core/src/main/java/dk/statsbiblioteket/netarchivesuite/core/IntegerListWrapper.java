package dk.statsbiblioteket.netarchivesuite.core;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
public class IntegerListWrapper {

    private ArrayList<Integer> values = new ArrayList<Integer>();

    public IntegerListWrapper(){
        
    }
    public ArrayList<Integer> getValues() {
        return values;
    }

    public void setValues(ArrayList<Integer> values) {
        this.values = values;
    }
    
    
}
