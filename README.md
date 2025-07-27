# Car Price Prediction App

A Clojure command-line application for searching, estimating, and predicting used car prices using both a static dataset for fast operations and real-time Marketcheck API data for enhanced precision and reliability. This app combines data analysis, regression techniques, and external data to provide accurate car price estimates and predictions.

This app is designed for anyone interested in buying or selling used cars, car enthusiasts, and analysts who want to understand and predict car market trends. Whether you are a car buyer looking for fair prices, a seller wanting to time your sale optimally, or a researcher analyzing car data, this app provides features to explore car listings, estimate current values, and predict future prices.

## Installation

This project was created using Leiningen, therefore application can be executed from the command line by following the steps described below.

Clone the repository:

```
git clone https://github.com/vsonja/car-price-prediction.git
```

Install project dependencies:

```
lein deps
```

You will need an API key from Marketcheck to fetch data and perform market-based predictions. Create a configuration file and add your API key:

```
(def api-key "MARKETCHECK_API_KEY")
```

Run the application:

```
lein run
```

The app uses MySQL to store saved searches by user ID. If you're using a local MySQL setup like XAMPP, make sure the MySQL module is running. You may need to create a database and relevant tables if they’re not already set up.

## User Stories

- As a user, I want to view all available cars so that I can explore my options.

- As a user, I want to search for specific car models so that I can find cars that meet my preferences.

- As a user, I want to save my search criteria so that I can easily access them later without re-entering details.

- As a user, I want to open my saved searches so that I can quickly review previous results.

- As a user, I want to estimate the current market price of a car so that I can make an informed buying or selling decision.

- As a user, I want to predict the price of a car for the upcoming months so that I can decide the best time to buy or sell a car.

## Usage

```
Welcome to the Car Price Prediction App!

Menu Options:
1. View all available cars
2. Search for a specific car
3. Open saved searches
4. Estimate current price
5. Predict prices for the upcoming months
6. Exit

Select an option: 
```

## Implementation Overview

### Phase 1: CSV dataset

Used dataset is a comprehensive collection of automotive information extracted from the popular automotive marketplace website, https://www.cars.com. This dataset comprises 3770 data points, each representing a unique car listing, and includes eight distinct features: brand, model, year, mileage, fuel_type, engine, transmission and price.

Initial functionality is built on this static CSV dataset to enable the following actions: view all cars in the dataset, search cars by user criteria, save and open searches, estimate current price.

- User can browse the entire dataset or subsets offline with the view-all functionality.

- The app filters dataset by make, model, year, mileage, fuel type, engine and transmission to enable quick searches.

- Searches are persisted by `user-id` (generated as a UUID) in MySQL database, allowing reuse and management of past queries.

    ![alt text](resources/searches.png)

- Current price estimation is implemented in two ways:
    - Comparative Value Method with Weighted Average provides a simple and intuitive way to estimate price by averaging prices of similar car, giving more weight to closer matches.
    - To improve accuracy and better model relationships between car features and prices, k-Nearest Neighbors (kNN) regression was implemented manually to predict price based on the `k` most similar cars. By using Gower's distance as similarity metric, kNN effectively handles mixed data types (numerical and categorical). The dissimilarity is calculated as follows for all types of features in a dataset:
    
        ![alt text](resources/distances.png)

        To choose the best number of neighbors `k`, the Elbow Method is applied. Function `elbow-method` plots the performance metric MAE (Mean Absolute Error) against `k` and returns optimal value.

        To evaluate the accuracy of the manually implemented k-NN regression model, the dataset was split into training and testing subsets (80/20 split). The following sample output shows the selected value of k, a test input example, and the predicted price:

        ```
        k = 2
        {:brand Chevrolet, :model Traverse Premier, :year 2020, :mileage 52000, :fuel_type Gasoline, :engine 3.6, :transmission Automatic, :price 35800}
        Predicted price: 35763.0
        ```        

This phase ensures quick testing, avoids API limits, and gives users a responsive experience. Due to limited API calls, these features still rely on the CSV dataset. However, API-based implementations of these features also exist and are used selectively where needed (for example, in price predictions and VIN-based queries).

### Phase 2: Real-Time Market Data via Marketcheck API

To make future price prediction more accurate, the app fetches live market data. The History API is the only Marketcheck endpoint that provides historical price data, but it works only with VIN (Vehicle Identification Number), not parameters like make, model, or year. Therefore, to build a monthly price series for a specific type of car, the app first needs to gather relevant VINs.

That’s why the following workflow is implemented:

- Using Search API, function `fetch-vins-from-search` fetches distinct VINs for the given make, model, and year.

- For each VIN, History API retrieves past listings with dates and prices. MarketCheck History API gives many similar entries because it's tracking the same car listed on different sites. To get meaningful monthly price trends, it was essential to iterate through pages. If there are more than 50 historical listings then `page` parameter can be used to iterate over them. Only odd pages were fetched to reduce API calls and avoid hitting rate limits.

- The app summarizes historical prices to form a monthly price series. Specifically, it groups the historical prices by months and calculates a representative price for each month by averaging all prices recorded during that period. This process transforms raw listing data into a clean, ordered time series of monthly prices that reflect how the car’s market value changes over time.

Linear regression is applied on the monthly price series to predict future prices (for example, 3 months ahead). Once the monthly price series is constructed, the app uses linear regression to model the trend of price changes over time. Linear regression fits a straight line to the historical monthly prices, estimating a relationship between time (in months) and price. This model captures the general direction and rate of price changes - depreciation, appreciation, or stability. Using the fitted regression line, the app can extrapolate prices into the future by extending the line beyond the latest available data point. For example, it can predict the estimated price 3 months from the current month by plugging the future month index into the regression equation. This approach provides a simple yet effective way to forecast future car prices based on observed historical trends.

While the initial dataset works well for fast prototyping, it lacks time-based price changes. For real future prediction, time-series data (monthly price changes) and real listings from multiple sources are neccessary. Marketcheck API provides this, though it's used selectively to avoid exceeding the API's rate limits. This hybrid approach balances performance and accuracy while minimizing external API usage for high-cost operations.

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
