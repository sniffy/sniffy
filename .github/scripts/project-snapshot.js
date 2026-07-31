'use strict';

const crypto = require('node:crypto');

const SNAPSHOT_SCHEMA = 'ai-delivery-project-snapshot/v1';
const DEFAULT_ORGANIZATION = 'sniffy';
const DEFAULT_PROJECT_NUMBER = 2;
const LIFECYCLE_FIELDS = Object.freeze([
  'Status',
  'Execution',
  'Executor',
  'Implementer',
  'Verifier',
  'Priority',
  'Ready timestamp',
  'Worker reference'
]);

function parseRepository(value) {
  const repository = String(value || '').trim();
  if (!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(repository)) {
    throw new Error('repository must use owner/name form.');
  }
  const [owner, name] = repository.split('/');
  return {repository, owner, name};
}

function parseItemNumber(value) {
  const number = Number(value);
  if (!Number.isInteger(number) || number < 1) {
    throw new Error('number must be a positive integer.');
  }
  return number;
}

function fieldValueMap(nodes) {
  const result = new Map();
  for (const value of nodes || []) {
    const name = value.field?.name;
    if (!name) continue;
    if (value.__typename === 'ProjectV2ItemFieldSingleSelectValue') {
      result.set(name, value.name ?? null);
    } else if (value.__typename === 'ProjectV2ItemFieldTextValue') {
      result.set(name, value.text ?? null);
    }
  }
  return result;
}

function stableStringify(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(',')}}`;
}

function revisionFor(material) {
  return `sha256:${crypto.createHash('sha256').update(stableStringify(material)).digest('hex')}`;
}

function normalizeSnapshot({organization, projectNumber, project, repository, target, item, observedAt}) {
  const values = fieldValueMap(item.fieldValues?.nodes);
  const fields = Object.fromEntries(LIFECYCLE_FIELDS.map(name => [name, values.has(name) ? values.get(name) : null]));
  const targetSnapshot = {
    repository,
    number: target.number,
    type: target.__typename,
    url: target.url,
    title: target.title,
    state: target.state
  };

  if (target.__typename === 'PullRequest') {
    targetSnapshot.head = target.headRefOid;
    targetSnapshot.base = target.baseRefName;
    targetSnapshot.draft = target.isDraft;
  }

  const material = {
    schema: SNAPSHOT_SCHEMA,
    project: {
      owner: organization,
      number: projectNumber,
      id: project.id,
      title: project.title
    },
    target: targetSnapshot,
    projectItem: {id: item.id},
    fields
  };

  return {
    ...material,
    observedAt,
    revision: revisionFor(material)
  };
}

async function readProjectSnapshot({
  github,
  repository,
  number,
  organization = DEFAULT_ORGANIZATION,
  projectNumber = DEFAULT_PROJECT_NUMBER,
  observedAt = new Date().toISOString()
}) {
  if (!github || typeof github.graphql !== 'function') {
    throw new Error('github GraphQL client is required.');
  }
  const parsedRepository = parseRepository(repository);
  const parsedNumber = parseItemNumber(number);

  const result = await github.graphql(`query(
    $organization: String!,
    $projectNumber: Int!,
    $owner: String!,
    $name: String!,
    $number: Int!
  ) {
    organization(login: $organization) {
      projectV2(number: $projectNumber) {
        id
        number
        title
      }
    }
    repository(owner: $owner, name: $name) {
      issueOrPullRequest(number: $number) {
        __typename
        ... on Issue {
          id
          number
          title
          url
          state
          projectItems(first: 50) { nodes { ...ProjectItem } }
        }
        ... on PullRequest {
          id
          number
          title
          url
          state
          isDraft
          baseRefName
          headRefOid
          projectItems(first: 50) { nodes { ...ProjectItem } }
        }
      }
    }
  }
  fragment ProjectItem on ProjectV2Item {
    id
    project { id }
    fieldValues(first: 100) { nodes {
      __typename
      ... on ProjectV2ItemFieldSingleSelectValue {
        name
        field { ... on ProjectV2SingleSelectField { name } }
      }
      ... on ProjectV2ItemFieldTextValue {
        text
        field { ... on ProjectV2Field { name } }
      }
    } }
  }`, {
    organization,
    projectNumber,
    owner: parsedRepository.owner,
    name: parsedRepository.name,
    number: parsedNumber
  });

  const project = result.organization?.projectV2;
  if (!project) throw new Error(`Project ${organization}/${projectNumber} was not found or is inaccessible.`);
  const target = result.repository?.issueOrPullRequest;
  if (!target) throw new Error(`${parsedRepository.repository}#${parsedNumber} was not found or is inaccessible.`);
  const item = target.projectItems?.nodes?.find(candidate => candidate.project?.id === project.id);
  if (!item) throw new Error(`${parsedRepository.repository}#${parsedNumber} is not represented in Project ${organization}/${projectNumber}.`);

  return normalizeSnapshot({
    organization,
    projectNumber,
    project,
    repository: parsedRepository.repository,
    target,
    item,
    observedAt
  });
}

module.exports = {
  DEFAULT_ORGANIZATION,
  DEFAULT_PROJECT_NUMBER,
  LIFECYCLE_FIELDS,
  SNAPSHOT_SCHEMA,
  fieldValueMap,
  normalizeSnapshot,
  parseItemNumber,
  parseRepository,
  readProjectSnapshot,
  revisionFor,
  stableStringify
};
