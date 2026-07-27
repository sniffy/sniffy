    @Rule public SniffyRule sniffyRule = new SniffyRule();

    @Test
    @DisableSockets
    public void testDisableSockets() throws IOException {
        try {
            new Socket("google.com", 22);
            fail("Sniffy should have thrown ConnectException");
        } catch (ConnectException e) {
            assertNotNull(e);
        }
    }
