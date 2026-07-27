        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <1>
        Sniffy.execute(
                () -> connection.createStatement().execute("SELECT 1 FROM DUAL")
        ).verify(SqlQueries.atMostOneQuery()); // <2>
