package com.core.mybatis;

import com.core.helper.Utility;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SqlM {
    private static final String MybatisConfigPath = "resources" + Utility.fileSeparator + "mybatis.xml";
    private static final Map<String, SqlSessionFactory> SqlSessionFactoryMap = new HashMap<String, SqlSessionFactory>();

    public static int exec(String sql) {
        return exec(sql, "");
    }


    public static int exec(String sql, Properties properties) {
        return getMapper(DefaultMapper.class, "", properties).exec(sql);
    }

    public static int exec(String sql, String dataSourceID, Properties properties) {
        return getMapper(DefaultMapper.class, dataSourceID, properties).exec(sql);
    }

    public static int exec(String sql, String dataSourceID) {
        return getMapper(DefaultMapper.class, dataSourceID).exec(sql);
    }

    public static Map<String, Object> getOne(String sql) {
        return getOne(sql, null);
    }

    public static Map<String, Object> getOne(String sql, String dataSourceID) {
        return getMapper(DefaultMapper.class, dataSourceID).getOne(sql);
    }

    public static List<Map<String, Object>> getList(String sql) {
        return getList(sql, null);
    }

    public static List<Map<String, Object>> getList(String sql, String dataSourceID) {
        return getMapper(DefaultMapper.class, dataSourceID).getList(sql);
    }

    public static SqlSession getSqlSession() {
        return getSqlSession("", true);
    }

    public static SqlSession getSqlSession(String dataSourceID) {
        return getSqlSession(dataSourceID, true);
    }

    public static SqlSession getSqlSession(boolean autoCommit) {
        return getSqlSession("", autoCommit);
    }

    public static SqlSession getSqlSession(String dataSourceID, boolean autoCommit) {
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceID);
        return sqlSessionFactory.openSession(autoCommit);
    }

    public static SqlSession getSqlSession(Properties properties) {
        return getSqlSession("", properties, true);
    }

    public static SqlSession getSqlSession(String dataSourceID, Properties properties) {
        return getSqlSession(dataSourceID, properties, true);
    }

    public static SqlSession getSqlSession(String dataSourceID, Properties properties, boolean autoCommit) {
        SqlSession sqlSession = null;
        Reader reader = null;
        if (dataSourceID == null || dataSourceID.equals(""))
            dataSourceID = "defaultDataSource";
        try {
            reader = Resources.getResourceAsReader(MybatisConfigPath);
            SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
            SqlSessionFactory sqlSessionFactory = builder.build(reader, dataSourceID, properties);
            sqlSession = sqlSessionFactory.openSession(autoCommit);
        } catch (IOException e) {
            Utility.getLogger(SqlM.class).error("", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Utility.getLogger(SqlM.class).error("", e);
            }
        }
        return sqlSession;
    }

    public static <T> T getMapper(Class<T> clazz) {
        return getMapper(clazz, "");
    }

    public static <T> T getMapper(Class<T> clazz, String dataSourceID) {
        SqlSession sqlSession = getSqlSession(dataSourceID);
        return (T) MapperProxy.bind(sqlSession.getMapper(clazz), sqlSession);
    }

    public static <T> T getMapper(Class<T> clazz, Properties properties) {
        return getMapper(clazz, "", properties);
    }

    public static <T> T getMapper(Class<T> clazz, String dataSourceID, Properties properties) {
        SqlSession sqlSession = getSqlSession(dataSourceID, properties);
        return (T) MapperProxy.bind(sqlSession.getMapper(clazz), sqlSession);
    }

    private static class MapperProxy<T> implements InvocationHandler {
        private T mapper;
        private SqlSession sqlSession;

        private MapperProxy(T mapper, SqlSession sqlSession) {
            this.mapper = mapper;
            this.sqlSession = sqlSession;
        }

        private static <T> T bind(T mapper, SqlSession sqlSession) {
            return (T) Proxy.newProxyInstance(mapper.getClass().getClassLoader(), mapper.getClass().getInterfaces(), new MapperProxy(mapper, sqlSession));
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object object = null;
            try {
                object = method.invoke(mapper, args);
            } catch (Exception e) {
                showError(e);
            } finally {
                sqlSession.close();
            }
            return object;
        }
    }

    static SqlSessionFactory getSqlSessionFactory(String dataSourceID) {
        if (dataSourceID == null || dataSourceID.equals(""))
            dataSourceID = "defaultDataSource";
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryMap.get(dataSourceID);
        if (sqlSessionFactory != null)
            return sqlSessionFactory;
        else {
            Reader reader = null;
            try {
                reader = Resources.getResourceAsReader(MybatisConfigPath);
                if (dataSourceID.equals("defaultDataSource"))
                    sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
                else
                    sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, dataSourceID);
            } catch (IOException e) {
                Utility.getLogger(SqlM.class).error("", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    Utility.getLogger(SqlM.class).error("", e);
                }
            }

            SqlSessionFactoryMap.put(dataSourceID, sqlSessionFactory);
            return sqlSessionFactory;
        }
    }

    static void showError(Exception e) {
        Utility.getLogger(SqlM.class).error(e.getCause().toString());
    }
}