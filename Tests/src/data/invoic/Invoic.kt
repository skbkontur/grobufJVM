@file:Suppress("unused")

package data.invoic

class AdditionalInformation(@JvmField var countryOfOriginNameCode: String,
                            @JvmField var dutyRegimeTypeCode: String,
                            @JvmField var specialConditionCode: Array<String>)

class AllowanceChargeInformation(@JvmField var allowanceOrChargeIdentifier: String,
                                 @JvmField var allowanceOrChargeIdentificationCode: String)

class SpecialServicesIdentification(@JvmField var specialServiceDescriptionCode: String,
                                    @JvmField var codeListIdentificationCode: String,
                                    @JvmField var codeListResponsibleAgencyCode: String,
                                    @JvmField var specialServiceDescription: Array<String>)

class AllowanceOrCharge(@JvmField var allowanceOrChargeCodeQualifier: String,
                        @JvmField var allowanceChargeInformation: AllowanceChargeInformation,
                        @JvmField var settlementMeansCode: String,
                        @JvmField var calculationSequenceCode: String,
                        @JvmField var specialServicesIdentification: SpecialServicesIdentification)

class DateTimePeriodGroup(@JvmField var functionCodeQualifier: String,
                          @JvmField var value: String,
                          @JvmField var formatCode: String)

class DateTimePeriod(@JvmField var dateTimePeriodGroup: DateTimePeriodGroup)

class DutyTaxFeeAccountDetail(@JvmField var dutyTaxFeeAccountCode: String,
                              @JvmField var codeListIdentificationCode: String,
                              @JvmField var codeListResponsibleAgencyCode: String)

class DutyTaxFeeDetail(@JvmField var dutyTaxFeeRateCode: String,
                       @JvmField var codeListIdentificationCode1: String,
                       @JvmField var codeListResponsibleAgencyCode1: String,
                       @JvmField var dutyTaxFeeRate: String,
                       @JvmField var dutyTaxFeeRateBasisCode: String,
                       @JvmField var codeListIdentificationCode2: String,
                       @JvmField var codeListResponsibleAgencyCode2: String)

class DutyTaxFeeType(@JvmField var dutyTaxFeeTypeNameCode: String,
                     @JvmField var codeListIdentificationCode: String,
                     @JvmField var codeListResponsibleAgencyCode: String,
                     @JvmField var dutyTaxFeeTypeName: String)

class DutyTaxFeeDetails(@JvmField var dutyTaxFeeFunctionCodeQualifier: String,
                        @JvmField var dutyTaxFeeType: DutyTaxFeeType,
                        @JvmField var dutyTaxFeeAccountDetail: DutyTaxFeeAccountDetail,
                        @JvmField var dutyTaxFeeAssessmentBasisValue: String,
                        @JvmField var dutyTaxFeeDetail: DutyTaxFeeDetail,
                        @JvmField var dutyTaxFeeCategoryCode: String,
                        @JvmField var partyTaxIdentifier: String,
                        @JvmField var calculationSequenceCode: String)

class MonetaryAmountGroup(@JvmField var monetaryAmountTypeCodeQualifier: String,
                          @JvmField var monetaryAmount: String,
                          @JvmField var currencyIdentificationCode: String,
                          @JvmField var currencyTypeCodeQualifier: String,
                          @JvmField var statusDescriptionCode: String)

class MonetaryAmount(@JvmField var monetaryAmountGroup: MonetaryAmountGroup)

class PercentageDetailsGroup(@JvmField var percentageTypeCodeQualifier: String,
                             @JvmField var percentage: String,
                             @JvmField var percentageBasisIdentificationCode: String,
                             @JvmField var codeListIdentificationCode: String,
                             @JvmField var codeListResponsibleAgencyCode: String)

class PercentageDetails(@JvmField var percentageDetailsGroup: PercentageDetailsGroup,
                        @JvmField var statusDescriptionCode: String)

class QuantityDetails(@JvmField var quantityTypeCodeQualifier: String,
                      @JvmField var quantity: String,
                      @JvmField var measurementUnitCode: String)

class Quantity(@JvmField var quantityDetails: QuantityDetails)

class Range(@JvmField var measurementUnitCode: String,
            @JvmField var rangeMinimumValue: String,
            @JvmField var rangeMaximumValue: String)

class RangeDetails(@JvmField var rangeTypeCodeQualifier: String,
                   @JvmField var range: Range)

class RateDetailsGroup(@JvmField var rateTypeCodeQualifier: String,
                       @JvmField var unitPriceBasisRate: String,
                       @JvmField var unitPriceBasisValue: String,
                       @JvmField var measurementUnitCode: String)

class RateDetails(@JvmField var rateDetailsGroup: RateDetailsGroup,
                  @JvmField var statusDescriptionCode: String)

class SG44(@JvmField var quantity: Quantity,
           @JvmField var rangeDetails: RangeDetails)

class SG45(@JvmField var percentageDetails: PercentageDetails,
           @JvmField var rangeDetails: RangeDetails)

class SG46(@JvmField var monetaryAmount: MonetaryAmount,
           @JvmField var rangeDetails: RangeDetails)

class SG47(@JvmField var rateDetails: RateDetails,
           @JvmField var rangeDetails: RangeDetails)

class SG48(@JvmField var dutyTaxFeeDetails: DutyTaxFeeDetails,
           @JvmField var monetaryAmount: MonetaryAmount)

class Invoic(@JvmField var allowanceOrCharge: AllowanceOrCharge,
             @JvmField var additionalInformation: Array<AdditionalInformation>,
             @JvmField var dateTimePeriod: Array<DateTimePeriod>,
             @JvmField var SG44: SG44,
             @JvmField var SG45: SG45,
             @JvmField var SG46: Array<SG46>,
             @JvmField var SG47: SG47,
             @JvmField var SG48: Array<SG48>)