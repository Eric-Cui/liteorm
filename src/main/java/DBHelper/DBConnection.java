package DBHelper;

import DBAnnotation.DatabaseTable;
import Entity.Account;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwei on 9/24/15.
 */

public class DBConnection {

    private Connection mConnection;
    private static Map<Class<?>, String> ormTypeMapping = new HashMap<Class<?>, String>();
    static {
        ormTypeMapping.put(String.class, "varchar(50");
        ormTypeMapping.put(Integer.class, "integer");
    }


    public DBConnection(DBConnectionProperties dbConnectionProperties) throws SQLException {
        mConnection = DriverManager.getConnection(dbConnectionProperties.getDataSourceUrl(),
                dbConnectionProperties.getDataSourceUserName(),
                dbConnectionProperties.getDataSourcePassword());
    }

    public void createTable(Class<?> tableClass) throws SQLException {
        /*
        Annotation[] annotations = tableClass.getAnnotations();
        for (Annotation annotation: annotations) {
            System.out.println("annotation " + annotation);
        }
        for (Method m: table.getDeclaredMethods()) {
            if (m.isAnnotationPresent(table)) {
                System.out.println("Annotation found " + m);
            }
        }*/
        //String tableName = tableClass.getSimpleName();
        if (tableClass.isAnnotationPresent(DatabaseTable.class)) {
            System.out.println("table class is annotated with DatabaseTable");
            DatabaseTable tableAnnotation = (DatabaseTable)tableClass.getAnnotation(DatabaseTable.class);
            String tableName = tableAnnotation.tableName();
            StringBuilder query = new StringBuilder();
            query.append("create table if not exists " + tableName + " ");
            Field[] fields = tableClass.getDeclaredFields();
            int fieldsLength = fields.length;
            for (int i = 0; i < fieldsLength; i++ ) {
                Field field = fields[i];
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                System.out.println("field name = " + fieldName + " type = " + fieldType);
                query.append(((i == 0) ? "(" : "") + fieldName + " " + ormTypeMapping.get(fieldType)  + ((i == fieldsLength-1) ? "));" : "),"));
            }
            System.out.println("create table command = " + query);
            Statement stmt = null;
            stmt = mConnection.createStatement();
            stmt.executeUpdate(query.toString());
            stmt.close();
        }


    }

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

    public <T> void insert(T entity) throws IllegalAccessException, InvocationTargetException, SQLException {
        Class<?> entityClass = entity.getClass();
        if (entityClass.isAnnotationPresent(DatabaseTable.class)) {
            DatabaseTable tableAnnotation = (DatabaseTable) entityClass.getAnnotation(DatabaseTable.class);
            String tableName = tableAnnotation.tableName();
            StringBuilder query = new StringBuilder();
            query.append("insert into " + tableName);
            List<String> columns = new ArrayList<String>();
            List<Object> values = new ArrayList<Object>();
            for (Field field : entityClass.getDeclaredFields()) {
                Object o = runGetter(field, entity);
                String fieldName = field.getName();
                columns.add(fieldName);
                Class<?> fieldClass = field.getType();
                System.out.println("o = " + o);
                if (fieldClass.equals(String.class)) {
                    values.add("'" + o + "'");
                } else {
                    values.add(o);
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
            System.out.println("insert record command = " + query);
            Statement stmt = null;
            stmt = mConnection.createStatement();
            stmt.executeUpdate(query.toString());
            stmt.close();
        }
    }
}
