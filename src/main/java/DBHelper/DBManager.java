package DBHelper;

import DBAnnotation.DatabaseField;
import DBAnnotation.DatabaseTable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by cuiwei on 9/24/15.
 */

public class DBManager {

    private Connection mConnection;
    private static Map<Class<?>, String> ormTypeMapping = new HashMap<Class<?>, String>();
    static {
        ormTypeMapping.put(String.class, "varchar(50)");
        ormTypeMapping.put(int.class, "integer");
    }

    public Connection getConnection(){
        return mConnection;
    }


    public DBManager(DBConnectionProperties dbConnectionProperties) throws SQLException {
        mConnection = DriverManager.getConnection(dbConnectionProperties.getDataSourceUrl(),
                dbConnectionProperties.getDataSourceUserName(),
                dbConnectionProperties.getDataSourcePassword());
    }

    public void getMetaData() throws SQLException {
        DatabaseMetaData databaseMetaData = mConnection.getMetaData();
        int majorVersion = databaseMetaData.getDatabaseMajorVersion();
        int minorVersion = databaseMetaData.getDatabaseMinorVersion();
        String productName = databaseMetaData.getDatabaseProductName();
        String productVersion = databaseMetaData.getDatabaseProductVersion();
        int driverMajorVersion = databaseMetaData.getDriverMajorVersion();
        int driverMinorVersion = databaseMetaData.getDriverMinorVersion();
        System.out.println("Database major version = " + majorVersion + " minor version = " + minorVersion);
        System.out.println("Product name = " + productName + " version = " + productVersion);
        System.out.println("JDBC Driver major version = " + driverMajorVersion + " minor version = " + minorVersion);
        System.out.println();

        System.out.println("Supports get generated keys = " + databaseMetaData.supportsGetGeneratedKeys());
        System.out.println("Supports group by = " + databaseMetaData.supportsGroupBy());
        System.out.println("Supports outer joins = " + databaseMetaData.supportsOuterJoins());
        System.out.println();
        
        String catalog = null;
        String schemaPattern = null;
        String tableNamePattern = null;
        String[] types = null;
        ResultSet tableRs = databaseMetaData.getTables(catalog, schemaPattern, tableNamePattern, types);
        System.out.println("Getting tables metadata ********");
        while (tableRs.next()) {
            String tableName = tableRs.getString(3);
            System.out.println("Database catalog = " + tableRs.getString(1) +
                                " table name = " + tableName);
            Map<String, String> columnNameTypes = getTableColumnDefinitions(databaseMetaData, tableName);
            for (Map.Entry<String, String> entry: columnNameTypes.entrySet()) {
                System.out.println("    column name = " + entry.getKey() + " type = " + entry.getValue());
            }
            List<String> primaryKeyList = getTablePrimaryKey(databaseMetaData, tableName);
            for (String primaryKey: primaryKeyList) {
                System.out.println("primary key column = " + primaryKey);
            }

        }
    }

    private List<String> getTablePrimaryKey(DatabaseMetaData databaseMetaData, String tableName) throws SQLException {
        ResultSet primaryKeyRs = databaseMetaData.getPrimaryKeys(null, null, tableName);
        List<String> primaryKeyList = new ArrayList<>();
        while (primaryKeyRs.next()) {
            primaryKeyList.add(primaryKeyRs.getString(4));
        }
        return primaryKeyList;
    }

    private Map<String, String> getTableColumnDefinitions(DatabaseMetaData databaseMetaData, String tableName) throws SQLException {
        ResultSet columnRs = databaseMetaData.getColumns(null, null, tableName, null);
        Map<String, String> columnNameTypes = new HashMap<>();
        while (columnRs.next()) {
            String columnName = columnRs.getString(4);
            int columnType = columnRs.getInt(5);
            columnNameTypes.put(columnName, getJdbcTypeName(columnType));

        }
        return columnNameTypes;
    }

    private String getJdbcTypeName(int jdbcType) {
        Field[] fields = java.sql.Types.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Integer value = (Integer) fields[i].get(null);
                if (value == jdbcType) {
                    return fields[i].getName();
                }
            } catch (IllegalAccessException e) {
            }
        }
        return null;
    }

    public void createTable(Class<?> tableClass) throws SQLException {
        if (tableClass.isAnnotationPresent(DatabaseTable.class)) {
            DatabaseTable tableAnnotation = (DatabaseTable)tableClass.getAnnotation(DatabaseTable.class);

            String tableName = tableAnnotation.tableName(); // get tableName from @DatabaseTable(tableName = "accounts")

            StringBuilder query = new StringBuilder();
            query.append("create table if not exists " + tableName + " ");

            Field[] fields = tableClass.getDeclaredFields(); // use reflection to get the defined fields of the class
            int fieldsLength = fields.length;
            String primaryKey = null;
            for (int i = 0; i < fieldsLength; i++ ) {
                Field field = fields[i];
                String fieldName = field.getName(); // id, name, ,password
                Class<?> fieldType = field.getType(); //int, String
                String fieldDesc = null;

                if (field.isAnnotationPresent(DatabaseField.class)) {
                    DatabaseField fieldAnnoation = (DatabaseField)field.getAnnotation(DatabaseField.class);
                    //get the databaseField from @DatabaseField(columnName = "name1", nullable = false)
                    boolean generatedId = fieldAnnoation.generatedId();
                    String columnName = fieldAnnoation.columnName();
                    boolean nullable = fieldAnnoation.nullable();
                    if (!columnName.equals("")) {
                        fieldName = columnName;
                    }
                    fieldDesc = fieldName + " " + ormTypeMapping.get(fieldType);
                    if (generatedId) {
                        fieldDesc += " NOT NULL AUTO_INCREMENT";
                        primaryKey = fieldName;
                    }

                    if (!nullable) {
                        fieldDesc += " NOT NULL";
                    }
                }
                query.append(((i == 0) ? "(" : "") + fieldDesc);
                if (i == fields.length - 1) {
                    if (primaryKey != null) {
                        query.append(", PRIMARY KEY (" + primaryKey + ")");
                    };
                    query.append(");");
                } else {
                    query.append(",");
                }
            }
            System.out.println("Executing creating table command: " + query);
            //create table if not exists accounts (id integer NOT NULL AUTO_INCREMENT,name1 varchar(50) NOT NULL,password1 varchar(50), PRIMARY KEY id);

            Statement stmt = null;
            stmt = mConnection.createStatement();
            stmt.executeUpdate(query.toString());
            stmt.close();
        }

    }

    // returning the field value of object o
    private <T> Object runGetter(Field field, T o) throws InvocationTargetException, IllegalAccessException {
        Class<?> entityClass = o.getClass();
        for (Method method : entityClass.getMethods())
        {
            if ((method.getName().startsWith("get")) && (method.getName().length() == (field.getName().length() + 3)))
            {
                if (method.getName().toLowerCase().endsWith(field.getName().toLowerCase()))
                {
                    return method.invoke(o);
                }
            }
        }
        return null;
    }

    private Method findMethod(Object object, String methodName) throws Exception
    {
        Class co = object.getClass();
        Method[] ml = co.getDeclaredMethods();
        for(Method m : ml)
        {
            if(m.getName().toLowerCase().contains(methodName))
                return m;
        }
        throw new IllegalArgumentException("cannot find " + methodName + " method of object");
    }

    public <T> int insert(T entity) throws IllegalAccessException, InvocationTargetException, SQLException {
        Class<?> entityClass = entity.getClass();
        int affectedRows = 0;
        if (entityClass.isAnnotationPresent(DatabaseTable.class)) {
            DatabaseTable tableAnnotation = (DatabaseTable) entityClass.getAnnotation(DatabaseTable.class);
            String tableName = tableAnnotation.tableName();
            StringBuilder query = new StringBuilder();
            query.append("insert into " + tableName);
            List<String> columns = new ArrayList<String>();
            List<Object> values = new ArrayList<Object>();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(DatabaseField.class)) {
                    DatabaseField fieldAnnoation = (DatabaseField)field.getAnnotation(DatabaseField.class);
                    boolean generatedId = fieldAnnoation.generatedId();
                    String columnName = fieldAnnoation.columnName();
                    boolean nullable = fieldAnnoation.nullable();
                    if (!generatedId) {
                        if (columnName.equals("")) {
                            columns.add(field.getName());
                        } else {
                            columns.add(columnName);
                        }
                        Object o = runGetter(field, entity);
                        Class<?> fieldClass = field.getType();
                        if (fieldClass.equals(String.class)) {
                            values.add("'" + o + "'");
                        } else {
                            values.add(o);
                        }
                    }
                } else {
                    String fieldName = field.getName();
                    columns.add(fieldName);
                    Object o = runGetter(field, entity);
                    Class<?> fieldClass = field.getType();
                    if (fieldClass.equals(String.class)) {
                        values.add("'" + o + "'");
                    } else {
                        values.add(o);
                    }
                }

            }
            query.append("(");
            for (int i = 0; i < columns.size(); i++) {
                query.append(columns.get(i) + ((i == columns.size() -1) ? "" : ","));
            }
            query.append(") values(");
            for (int i = 0; i < values.size(); i++) {
                query.append(values.get(i) + ((i == values.size() -1) ? "": ","));
            }
            query.append(");");
            System.out.println("Executing inserting record command: " + query);
            //Statement stmt = null;
            //stmt = mConnection.createStatement();
            //stmt.executeUpdate(query.toString());
            try (
            PreparedStatement stmt = mConnection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            ) {
                affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("inserting record failed, no rows affected");
                }
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        findMethod(entity, "setid").invoke(entity, generatedKeys.getInt(1));
                    }
                } catch (Exception e) {
                    throw new SQLException("inserting record failed, no ID obtained");
                }
            }
        }
        return affectedRows;
    }

    public <T> List<T> queryForAll(Class<T> entityClass) throws SQLException, IllegalAccessException, InstantiationException {
        List<T> tList = new ArrayList<T>();
        if (entityClass.isAnnotationPresent(DatabaseTable.class)) {

            DatabaseTable tableAnnotation = (DatabaseTable) entityClass.getAnnotation(DatabaseTable.class);

            String tableName = tableAnnotation.tableName(); // get tableName from @DatabaseTable(tableName = "accounts")
            Statement stmt = mConnection.createStatement();
            ResultSet rs = stmt.executeQuery("select * from " + tableName);

            while (rs.next()) {
                T element = entityClass.newInstance();
                for (Field field : entityClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(DatabaseField.class)) {
                        DatabaseField fieldAnnoation = (DatabaseField) field.getAnnotation(DatabaseField.class);
                        boolean generatedId = fieldAnnoation.generatedId();
                        String columnName = fieldAnnoation.columnName();
                        if (columnName.equals("")) {
                            columnName = field.getName();
                        }
                        Class<?> fieldClass = field.getType();
                        Object val = null;
                        if (fieldClass.equals(String.class)) {
                            val = rs.getString(columnName);
                        } else if (fieldClass.equals(int.class)){
                            val = rs.getInt(columnName);

                        }
                        field.set(element, val);
                    }
                }
                tList.add(element);
            }
        }
        return tList;
    }

    public <T> int delete(T entity) throws Exception {
        int id = (int)findMethod(entity, "getid").invoke(entity);
        Class<?> entityClass = entity.getClass();
        int rowsAffected = 0;
        if (entityClass.isAnnotationPresent(DatabaseTable.class)) {
            DatabaseTable tableAnnotation = (DatabaseTable) entityClass.getAnnotation(DatabaseTable.class);
            String tableName = tableAnnotation.tableName();
            StringBuilder query = new StringBuilder();
            String cmd = "delete from " + tableName + " where id = " + id;
            System.out.println("Executing deleting record command: " + cmd);
            query.append(cmd);
            Statement stmt = mConnection.createStatement();
            rowsAffected = stmt.executeUpdate(query.toString());
            stmt.close();
        }
        return rowsAffected;
    }

    public <T> int update(T entity) throws Exception {
        int rowsAffected = 0;
        int id = (int)findMethod(entity, "getid").invoke(entity);
        Class<?> entityClass = entity.getClass();
        // "update accounts set name1='Adam' , password1='cdef' where id=3
        if (entityClass.isAnnotationPresent(DatabaseTable.class)) {
            DatabaseTable tableAnnotation = (DatabaseTable) entityClass.getAnnotation(DatabaseTable.class);
            String tableName = tableAnnotation.tableName();
            StringBuilder query = new StringBuilder();
            query.append("update " + tableName + " set ");
            boolean oneUpdated = false;
            for (Field field: entityClass.getDeclaredFields()) {
                String columnName;
                if (field.isAnnotationPresent(DatabaseField.class)) {
                    DatabaseField fieldAnnoation = (DatabaseField) field.getAnnotation(DatabaseField.class);
                    if (fieldAnnoation.generatedId()) // skip updating the autogenerated ID field
                        continue;
                    columnName = fieldAnnoation.columnName();
                    if (columnName.isEmpty()) {
                        columnName = field.getName();
                    }
                } else {
                    columnName = field.getName();
                }
                if (oneUpdated)
                    query.append(" , ");
                else
                    oneUpdated = true;
                query.append(columnName + " = ");
                Class<?> fieldClass = field.getType();
                Object o = runGetter(field, entity);
                if (fieldClass.equals(String.class)) {
                    query.append("'" + o + "'");
                } else {
                    query.append(o);
                }
            }
            query.append(" where id = " + id);
            System.out.println("Executing updating record command: " + query);
            Statement stmt = mConnection.createStatement();
            rowsAffected = stmt.executeUpdate(query.toString());
            stmt.close();
        }
        return rowsAffected;
    }

    public <V> void executeTransaction(Callable<V> callable) throws SQLException {
        mConnection.setAutoCommit(false);
        Savepoint savePoint = null;
        try {
            savePoint = mConnection.setSavepoint("savepoint_begin");
            callable.call();
            mConnection.commit();
        } catch (Exception ex) {
            mConnection.rollback(savePoint);
        }
    }
}
