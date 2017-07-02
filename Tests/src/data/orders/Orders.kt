@file:Suppress("unused")

package data.orders

import grobuf.Decimal
import java.util.*

class AdditionalInformation(@JvmField var countryOfOriginNameCode: Int?,
                            @JvmField var dutyRegimeTypeCode: Byte,
                            @JvmField var specialConditionCode: Array<String>)

class AllowanceChargeInformation(@JvmField var allowanceOrChargeIdentifier: Int,
                                 @JvmField var allowanceOrChargeIdentificationCode: String)

class SpecialServicesIdentification(@JvmField var specialServiceDescriptionCode: BooleanArray,
                                    @JvmField var codeListIdentificationCode: Byte,
                                    @JvmField var codeListResponsibleAgencyCode: Boolean?,
                                    @JvmField var specialServiceDescription: Array<Boolean?>,
                                    @JvmField var id: UUID,
                                    @JvmField var childrenIds: Array<UUID>)

class AllowanceOrCharge(@JvmField var allowanceOrChargeCodeQualifier: Byte,
                        @JvmField var allowanceChargeInformation: AllowanceChargeInformation,
                        @JvmField var settlementMeansCode: Short,
                        @JvmField var calculationSequenceCode: Short?,
                        @JvmField var specialServicesIdentification: SpecialServicesIdentification)

class DateTimePeriodGroup(@JvmField var functionCodeQualifier: Byte?,
                          @JvmField var value: Date,
                          @JvmField var formatCode: Byte)

class DateTimePeriod(@JvmField var dateTimePeriodGroup: DateTimePeriodGroup)

class DutyTaxFeeAccountDetail(@JvmField var dutyTaxFeeAccountCode: Short?,
                              @JvmField var codeListIdentificationCode: Short,
                              @JvmField var codeListResponsibleAgencyCode: Char)

class DutyTaxFeeDetail(@JvmField var dutyTaxFeeRateCode: Char?,
                       @JvmField var codeListIdentificationCode1: Array<Int?>,
                       @JvmField var codeListResponsibleAgencyCode1: IntArray,
                       @JvmField var dutyTaxFeeRate: Array<Long?>,
                       @JvmField var dutyTaxFeeRateBasisCode: Long,
                       @JvmField var codeListIdentificationCode2: Long?,
                       @JvmField var codeListResponsibleAgencyCode2: LongArray)

class DutyTaxFeeType(@JvmField var dutyTaxFeeTypeNameCode: Array<Byte?>,
                     @JvmField var codeListIdentificationCode: String,
                     @JvmField var codeListResponsibleAgencyCode: Array<Short?>,
                     @JvmField var dutyTaxFeeTypeName: Double?)

class DutyTaxFeeDetails(@JvmField var dutyTaxFeeFunctionCodeQualifier: ByteArray,
                        @JvmField var dutyTaxFeeType: DutyTaxFeeType,
                        @JvmField var dutyTaxFeeAccountDetail: DutyTaxFeeAccountDetail,
                        @JvmField var dutyTaxFeeAssessmentBasisValue: DoubleArray,
                        @JvmField var dutyTaxFeeDetail: DutyTaxFeeDetail,
                        @JvmField var dutyTaxFeeCategoryCode: FloatArray,
                        @JvmField var partyTaxIdentifier: Float,
                        @JvmField var calculationSequenceCode: String)

class MonetaryAmountGroup(@JvmField var monetaryAmountTypeCodeQualifier: Decimal,
                          @JvmField var monetaryAmount: Int?,
                          @JvmField var currencyIdentificationCode: Boolean?,
                          @JvmField var currencyTypeCodeQualifier: Date,
                          @JvmField var statusDescriptionCode: String)

class MonetaryAmount(@JvmField var monetaryAmountGroup: MonetaryAmountGroup)

class PercentageDetailsGroup(@JvmField var percentageTypeCodeQualifier: Double?,
                             @JvmField var percentage: String,
                             @JvmField var percentageBasisIdentificationCode: LongArray,
                             @JvmField var codeListIdentificationCode: Boolean,
                             @JvmField var codeListResponsibleAgencyCode: Array<Long?>)

class PercentageDetails(@JvmField var percentageDetailsGroup: PercentageDetailsGroup,
                        @JvmField var statusDescriptionCode: Array<Decimal>)

class QuantityDetails(@JvmField var quantityTypeCodeQualifier: Array<Date>,
                      @JvmField var quantity: ShortArray,
                      @JvmField var measurementUnitCode: DoubleArray)

class Quantity(@JvmField var quantityDetails: QuantityDetails)

class Range(@JvmField var measurementUnitCode: CharArray,
            @JvmField var rangeMinimumValue: Array<Byte?>,
            @JvmField var rangeMaximumValue: FloatArray)

class RangeDetails(@JvmField var rangeTypeCodeQualifier: Array<Char?>,
                   @JvmField var range: Range)

class RateDetailsGroup(@JvmField var rateTypeCodeQualifier: ShortArray,
                       @JvmField var unitPriceBasisRate: Int,
                       @JvmField var unitPriceBasisValue: Long,
                       @JvmField var measurementUnitCode: UUID)

class RateDetails(@JvmField var rateDetailsGroup: RateDetailsGroup,
                  @JvmField var statusDescriptionCode: Byte)

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

class Orders(@JvmField var allowanceOrCharge: AllowanceOrCharge,
             @JvmField var additionalInformation: Array<AdditionalInformation>,
             @JvmField var dateTimePeriod: Array<DateTimePeriod>,
             @JvmField var SG44: SG44,
             @JvmField var SG45: SG45,
             @JvmField var SG46: Array<SG46>,
             @JvmField var SG47: SG47,
             @JvmField var SG48: Array<SG48>)