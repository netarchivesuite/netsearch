package dk.statsbiblioteket.netarchivesuite.core;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StringListWrapper {

    private ArrayList<String> values = new ArrayList<String>();
    
    public StringListWrapper(){
        
    }
    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }
    
    
}
