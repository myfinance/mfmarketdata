# mfmarketdata

Service to load marketdata from providers like Alphavantage.

A price for an Instrument != 0 will never be overridden by a Dataimport. Data/Price-tuple will only be added to the instrument.
There will be no manipulation of the values. If a price comes in USD it will be saved and publish in USD. There will be no conversion from currencies.
Tere is only one exception:
In case of a wrong value (or no value) you can make an override