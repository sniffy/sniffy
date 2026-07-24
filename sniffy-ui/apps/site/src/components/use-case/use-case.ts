export type UseCaseDomainAccent = 'sql' | 'network' | 'traffic';
export type SiteRoute = `/${string}/`;
export type SiteAsset = `/${string}`;

export type UseCaseRelatedDoc = {
  readonly description: string;
  readonly href: SiteRoute;
  readonly label: string;
};

export type UseCaseMetadata = {
  readonly accent: UseCaseDomainAccent;
  readonly description: string;
  readonly relatedDocs: readonly [UseCaseRelatedDoc, ...UseCaseRelatedDoc[]];
  readonly route: SiteRoute;
  readonly social: {
    readonly description: string;
    readonly image: SiteAsset;
    readonly title: string;
  };
  readonly title: string;
};

export type UseCaseCodeExample = {
  readonly code: string;
  readonly label: string;
  readonly language: string;
  readonly source?: {
    readonly label: string;
    readonly path: string;
    readonly region?: string;
  };
};

export type UseCasePageContent = {
  readonly capabilities: readonly [
    {
      readonly description: string;
      readonly title: string;
    },
    ...Array<{
      readonly description: string;
      readonly title: string;
    }>,
  ];
  readonly codeExamples: readonly [UseCaseCodeExample, ...UseCaseCodeExample[]];
  readonly cta: {
    readonly body: string;
    readonly primary: { readonly href: SiteRoute; readonly label: string };
    readonly secondary: { readonly href: SiteRoute; readonly label: string };
    readonly title: string;
  };
  readonly eyebrow: string;
  readonly hero: {
    readonly body: string;
    readonly proofPoints: readonly [string, ...string[]];
    readonly title: string;
  };
  readonly metadata: UseCaseMetadata;
  readonly problem: {
    readonly body: readonly [string, ...string[]];
    readonly title: string;
  };
  readonly productResult: {
    readonly alt: string;
    readonly caption: string;
    readonly height: number;
    readonly highlights: readonly [string, ...string[]];
    readonly provenance: string;
    readonly src: SiteAsset;
    readonly width: number;
  };
  readonly solution: {
    readonly body: readonly [string, ...string[]];
    readonly title: string;
  };
};

function requireText(value: string, field: string): void {
  if (value.trim().length === 0) {
    throw new Error(`Use-case metadata field "${field}" must not be empty.`);
  }
}

function requireSiteRoute(value: string, field: string): void {
  if (!/^\/(?:[^/?#]+\/)+$/.test(value)) {
    throw new Error(
      `Use-case metadata field "${field}" must be a canonical trailing-slash site route.`,
    );
  }
}

function validateMetadata(metadata: UseCaseMetadata): void {
  requireText(metadata.title, 'title');
  requireText(metadata.description, 'description');
  requireSiteRoute(metadata.route, 'route');
  if (!metadata.route.startsWith('/use-cases/')) {
    throw new Error('Use-case metadata field "route" must live below /use-cases/.');
  }
  if (!['sql', 'network', 'traffic'].includes(metadata.accent)) {
    throw new Error('Use-case metadata field "accent" is not a supported domain accent.');
  }
  if (metadata.relatedDocs.length === 0) {
    throw new Error('Use-case metadata field "relatedDocs" must contain at least one link.');
  }
  for (const [index, relatedDoc] of metadata.relatedDocs.entries()) {
    requireText(relatedDoc.label, `relatedDocs[${index}].label`);
    requireText(relatedDoc.description, `relatedDocs[${index}].description`);
    requireSiteRoute(relatedDoc.href, `relatedDocs[${index}].href`);
    if (!relatedDoc.href.startsWith('/docs/')) {
      throw new Error(
        `Use-case metadata field "relatedDocs[${index}].href" must link to current documentation.`,
      );
    }
  }
  requireText(metadata.social.title, 'social.title');
  requireText(metadata.social.description, 'social.description');
  if (!metadata.social.image.startsWith('/img/')) {
    throw new Error('Use-case metadata field "social.image" must be a site image path.');
  }
}

export function defineUseCase<const T extends UseCasePageContent>(content: T): Readonly<T> {
  validateMetadata(content.metadata);
  requireText(content.eyebrow, 'eyebrow');
  requireText(content.hero.title, 'hero.title');
  requireText(content.hero.body, 'hero.body');
  requireText(content.problem.title, 'problem.title');
  requireText(content.solution.title, 'solution.title');
  requireText(content.cta.title, 'cta.title');
  if (content.capabilities.length === 0 || content.codeExamples.length === 0) {
    throw new Error('A use-case page must include capabilities and code examples.');
  }
  if (
    !Number.isInteger(content.productResult.width) ||
    !Number.isInteger(content.productResult.height) ||
    content.productResult.width <= 0 ||
    content.productResult.height <= 0
  ) {
    throw new Error('Use-case product media must declare positive integer dimensions.');
  }

  return Object.freeze(content);
}
