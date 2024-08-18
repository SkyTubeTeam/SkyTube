package free.rm.skytube.businessobjects.model;

public enum Status {
    OK (0),
    ACCOUNT_TERMINATED (1),
    NOT_EXISTS (2);

    public final int code;
    Status(int code) {
        this.code = code;
    }


    public static Status lookup(Long code) {
        if (code != null){
            return lookup(code.intValue());
        } else {
            throw new IllegalArgumentException("Missing code: "+ code);
        }
    }

    public static Status lookup(int code) {
        switch (code) {
            case 0:
                return OK;
            case 1:
                return ACCOUNT_TERMINATED;
            case 2:
                return NOT_EXISTS;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}

