    @Rule public SniffyRule sniffyRule = new SniffyRule();

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    @SqlExpectation(count = @Count(1))
    public void testExpectedOneQueryGotOne() throws SQLException {
        DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa").createStatement().execute("SELECT 1 FROM DUAL"); // <4>
    }

    @Test
    @SqlExpectation(count = @Count(max = 1), query = SqlStatement.SELECT)
    public void testExpectedNotMoreThanOneSelectGotTwo() throws SQLException {
        try (Statement statement = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa").createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
            statement.execute("SELECT 2 FROM DUAL");
        }
        thrown.expect(WrongNumberOfQueriesError.class);
    }
