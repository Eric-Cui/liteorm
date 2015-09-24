package Entity;

import DBAnnotation.DatabaseField;
import DBAnnotation.DatabaseTable;

/**
 * Created by cuiwei on 9/24/15.
 */
@DatabaseTable(tableName = "accounts")
public class Account {


    @DatabaseField(id = true)
    private String name;

    private String password;

    public Account() {
    }

    public Account(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "name = " + name + " password = " + password;
    }
}