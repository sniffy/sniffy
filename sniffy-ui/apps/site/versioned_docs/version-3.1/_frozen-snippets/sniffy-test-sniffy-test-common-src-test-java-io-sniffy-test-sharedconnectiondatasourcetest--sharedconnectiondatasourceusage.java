        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(targetDataSource); // <1>

        sharedConnectionDataSource.setCurrentThreadAsMaster(); // <2>

        try (Connection masterConnection = sharedConnectionDataSource.getConnection(); // <3>
             Connection slaveConnection = newSingleThreadExecutor().submit(
                     (Callable<Connection>) sharedConnectionDataSource::getConnection).get() // <4>
        ) {
            assertEquals(masterConnection, slaveConnection); // <5>
        } finally {
            sharedConnectionDataSource.resetMasterConnection(); // <6>
        }
