import type {
  AnalysisState,
  CompliancePortfolioSummary,
  ComplianceResult,
  ComplianceSeverity,
  InventoryRefreshState,
  InventoryStatus,
  LicensePresence,
  RefreshDiagnosticsSnapshot,
  RepositoryComplianceDetail,
  RepositoryRefreshOutcome,
  RepositorySummary,
  RepositoryVisibility,
} from '../src/api'
import type { components } from './generated/schema'

type Schemas = components['schemas']

type Equal<A, B> =
  (<T>() => T extends A ? 1 : 2) extends
  (<T>() => T extends B ? 1 : 2)
    ? (<T>() => T extends B ? 1 : 2) extends
      (<T>() => T extends A ? 1 : 2)
      ? true
      : false
    : false

type Expect<T extends true> = T
type NoExtraFrontendKeys<Frontend, Backend> =
  Exclude<keyof Frontend, keyof Backend> extends never ? true : false

type _AnalysisState = Expect<Equal<AnalysisState, Schemas['AnalysisState']>>
type _RepositoryVisibility = Expect<Equal<RepositoryVisibility, Schemas['RepositoryVisibility']>>
type _RepositoryRefreshOutcome = Expect<Equal<RepositoryRefreshOutcome, Schemas['RepositoryRefreshOutcome']>>
type _LicensePresence = Expect<Equal<LicensePresence, Schemas['LicensePresence']>>
type _InventoryRefreshState = Expect<Equal<InventoryRefreshState, Schemas['InventoryRefreshState']>>
type _ComplianceResult = Expect<Equal<ComplianceResult, Schemas['RepositoryRuleEvaluationResult']>>
type _ComplianceSeverity = Expect<Equal<ComplianceSeverity, Schemas['RepositoryRuleSeverity']>>

type _RepositorySummaryKeys = Expect<
  NoExtraFrontendKeys<RepositorySummary, Schemas['RepositorySummary']>
>
type _InventoryStatusKeys = Expect<
  NoExtraFrontendKeys<InventoryStatus, Schemas['InventoryStatus']>
>
type _ComplianceSummaryKeys = Expect<
  NoExtraFrontendKeys<CompliancePortfolioSummary, Schemas['CompliancePortfolioSummary']>
>
type _RepositoryComplianceDetailKeys = Expect<
  NoExtraFrontendKeys<RepositoryComplianceDetail, Schemas['RepositoryComplianceDetail']>
>
type _RefreshDiagnosticsKeys = Expect<
  NoExtraFrontendKeys<RefreshDiagnosticsSnapshot, Schemas['RefreshDiagnosticsSnapshot']>
>

type _InventoryStateValue = Expect<
  Equal<InventoryStatus['state'], NonNullable<Schemas['InventoryStatus']['state']>>
>
type _InventoryRepositoryCountValue = Expect<
  Equal<InventoryStatus['repositoryCount'], NonNullable<Schemas['InventoryStatus']['repositoryCount']>>
>
type _InventoryReusedCountValue = Expect<
  Equal<InventoryStatus['reusedCount'], NonNullable<Schemas['InventoryStatus']['reusedCount']>>
>
type _InventoryNewCountValue = Expect<
  Equal<InventoryStatus['newCount'], NonNullable<Schemas['InventoryStatus']['newCount']>>
>
type _InventoryChangedCountValue = Expect<
  Equal<InventoryStatus['changedCount'], NonNullable<Schemas['InventoryStatus']['changedCount']>>
>
type _InventoryScheduledCountValue = Expect<
  Equal<InventoryStatus['scheduledCount'], NonNullable<Schemas['InventoryStatus']['scheduledCount']>>
>

// This file intentionally contains type-level assertions only.
// tsc --noEmit is the contract check.
export {}
