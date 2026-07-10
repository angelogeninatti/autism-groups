package datamodel;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class User {

    private int id;

    private String prolific_id;

    private String unique_id;

    private SPQResult SPQResult;

    private BigFResult BigFResult;

    private int attentionFailed;

    private String completitionCode;

    public User (int id, String prolific_id, String unique_id, SPQResult SPQResult, BigFResult BigFResult, int attentionFailed, String completitionCode) {
        this.id = id;
        this.prolific_id = prolific_id;
        this.unique_id = unique_id;
        this.SPQResult = SPQResult;
        this.BigFResult = BigFResult;
        this.attentionFailed = attentionFailed;
        this.completitionCode = completitionCode;
    }

    public int getId(){
        return id;
    }

    public String getProlificId(){
        return prolific_id;
    }

    public String getUniqueId(){
        return unique_id;
    }

    public SPQResult getSPQResult(){
        return SPQResult;
    }

    public BigFResult getBigFResult(){
        return BigFResult;
    }

    public int getAttentionFailed(){
        return attentionFailed;
    }

    public String getCompletitionCode(){
        return completitionCode;
    }
}