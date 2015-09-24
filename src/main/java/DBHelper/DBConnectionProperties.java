package DBHelper;

/**
 * Created by cuiwei on 9/24/15.
 */
public class DBConnectionProperties {
    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDataSourceUserName() {
        return dataSourceUserName;
    }

    public void setDataSourceUserName(String dataSourceUserName) {
        this.dataSourceUserName = dataSourceUserName;
    }

    public String getDataSourcePassword() {
        return dataSourcePassword;
    }

    public void setDataSourcePassword(String dataSourcePassword) {
        this.dataSourcePassword = dataSourcePassword;
    }

    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    public void setDataSourceUrl(String dataSourceUrl) {
        this.dataSourceUrl = dataSourceUrl;
    }

    public DBConnectionProperties(String driverClassName, String dataSourceUserName, String dataSourcePassword, String dataSourceUrl) {
        this.driverClassName = driverClassName;
        this.dataSourceUserName = dataSourceUserName;
        this.dataSourcePassword = dataSourcePassword;
        this.dataSourceUrl = dataSourceUrl;
    }

    @Override
    public String toString()
    {
        return "driverClassName = " + driverClassName + " datasource (url = " + dataSourceUrl + " user = " + dataSourceUserName + " password " + dataSourcePassword + ")";
    }

    private String driverClassName;
    private String dataSourceUserName;
    private String dataSourcePassword;
    private String dataSourceUrl;


}
