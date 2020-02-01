package frame.http;


import java.io.Serializable;

public class BaseModel implements Serializable {


    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
