        Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <1>
        Spy<?> spy = Sniffy.spy(); // <2>
        connection.createStatement().execute("SELECT 1 FROM DUAL"); // <3>
        spy.verify(SqlQueries.atMostOneQuery()); // <4>
        spy.verify(SqlQueries.noneQueries().otherThreads()); // <5>
