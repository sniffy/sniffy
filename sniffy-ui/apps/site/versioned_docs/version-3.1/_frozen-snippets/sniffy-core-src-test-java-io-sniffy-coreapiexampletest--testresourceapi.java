        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <1>
        try (@SuppressWarnings("unused") Spy s = Sniffy. // <2>
                expect(SqlQueries.atMostOneQuery()).
                expect(SqlQueries.noneQueries().otherThreads());
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
        }
