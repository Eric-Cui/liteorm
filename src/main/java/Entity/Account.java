package Entity;

import DBAnnotation.DatabaseField;
import DBAnnotation.DatabaseTable;

/**
 * Created by cuiwei on 9/24/15.
 */
@DatabaseTable(tableName = "accounts")
public class Account {


    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(columnName = "name1", nullable = false)
    public String name;

    @DatabaseField(columnName = "password1")
    public String password;

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
        return "id = " + id + " name = " + name + " password = " + password;
    }
}