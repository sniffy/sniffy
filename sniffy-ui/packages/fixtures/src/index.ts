import { http, HttpResponse } from 'msw';
import type { ConnectionRegistry, RequestStats, TopSql } from '@sniffy/api';

export const registryFixture: ConnectionRegistry = {
  persistent: false,
  sockets: [
    { host: 'en.wikipedia.org', port: 443, status: 100 },
    { host: '192.168.99.100', port: 3306, status: -50 },
  ],
  dataSources: [{ url: 'jdbc:h2:mem:/something:', userName: 'sa', status: 0 }],
};

export const longStack = Array.from(
  { length: 28 },
  (_, index) => `at io.sniffy.example.Service.call(Service.java:${40 + index})`,
).join('\n');

export const requestFixture: RequestStats = {
  time: 84,
  timeToFirstByte: 21,
  executedQueries: [
    {
      query:
        'select owner0_.id, owner0_.first_name, owner0_.last_name from owners owner0_ where owner0_.last_name like ?',
      time: 12,
      invocations: 2,
      rows: 4,
      bytesDown: 814,
      bytesUp: 92,
      stackTrace: longStack,
    },
  ],
  networkConnections: [
    {
      host: 'database.internal',
      port: 5432,
      time: 9,
      bytesDown: 814,
      bytesUp: 92,
      stackTrace: longStack,
    },
  ],
  exceptions: [
    {
      className: 'java.lang.IllegalStateException',
      message: 'Example failure',
      stackTrace: longStack,
    },
  ],
};

export const topSqlFixture: TopSql[] = [
  {
    sql: 'select * from visits where pet_id = ?',
    timer: {
      count: 104,
      min: 0,
      median: 0,
      mean: 274962,
      max: 1000000,
      p75: 1000000,
      p95: 1000000,
      p99: 1000000,
    },
  },
  {
    sql: 'select * from owners where last_name like ?',
    timer: {
      count: 8,
      min: 1000000,
      median: 1000000,
      mean: 1481153,
      max: 3000000,
      p75: 2000000,
      p95: 2000000,
      p99: 3000000,
    },
  },
];

export const mswHandlers = [
  http.get('*/connectionregistry/', () => HttpResponse.json(registryFixture)),
  http.all('*/connectionregistry/*', () => new HttpResponse(null, { status: 201 })),
  http.get('*/topsql/', () => HttpResponse.json(topSqlFixture)),
  http.delete('*/topsql/', () => new HttpResponse(null, { status: 201 })),
  http.get('*/request/*', () => HttpResponse.json(requestFixture)),
];
