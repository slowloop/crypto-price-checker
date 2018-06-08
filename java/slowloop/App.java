package slowloop;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Controller
@EnableAutoConfiguration
public class App implements ErrorController {

    public String name, price, line;
    public double change_24h, change_1h;
    public int rank;
    public List<String> timestamps;

    public static void main(String[] args)
    {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping("/error")
    public String error(Model model) {

        model.addAttribute("message", "Page doesn't exist!");

        return "error/404";
    }

    //Grab the price information of a coin (specified by the user) from coinmarketcap.com and cryptocompare.com.
    @GetMapping("")
    public String coinFinder(@RequestParam(value="name", required=false) String coin, Model model) {

        RestTemplate restTemplate = new RestTemplate();
        boolean found;

        try{
            CoinMarketCap[] cryptocurrencyInfo = restTemplate.getForObject("https://api.coinmarketcap.com/v1/ticker/" + coin.replaceAll("\\W", "-"), CoinMarketCap[].class);

            String price;

            //Grab current price in USD.
            //Format coin price. Allow only two digits after the decimal point.
            if(Double.toString(cryptocurrencyInfo[0].price_usd)
                    .substring(0, Double.toString(cryptocurrencyInfo[0].price_usd)
                                .indexOf(".")).length() >= 3){
                price = String.format("%.0f", cryptocurrencyInfo[0].price_usd);
            }
            else {
                price = String.format("%.2f", cryptocurrencyInfo[0].price_usd);
            }

            //Get the coin's 1 week historical price data from cryptocompare.com.
            Date date = new Date();
            int count = 0;
            String time;
            timestamps = new ArrayList<String>();

            while(count < 8){
                count++;

                time = Long.toString((date.getTime()/1000) - (86400 * count));

                String prices = restTemplate.getForObject("https://min-api.cryptocompare.com/data/pricehistorical?fsym=" + (cryptocurrencyInfo[0].symbol).toUpperCase() + "&tsyms=USD&ts=" + time, String.class);

                timestamps.add(prices.substring(prices.lastIndexOf(":")+1, prices.length()-2));
            }

            found = true;
            this.name = cryptocurrencyInfo[0].name;
            change_24h = cryptocurrencyInfo[0].percent_change_24h;
            change_1h = cryptocurrencyInfo[0].percent_change_1h;
            this.price = "$" + price;
            this.rank = cryptocurrencyInfo[0].rank;
            line = timestamps.get(7);

            model.addAttribute("coin", coin);
            model.addAttribute("line", line);
            model.addAttribute("list", timestamps);
            model.addAttribute("found", found);
            model.addAttribute("name", this.name);
            model.addAttribute("change_24h", change_24h);
            model.addAttribute("change_1h", change_1h);
            model.addAttribute("price", this.price);
            model.addAttribute("rank", this.rank);
            model.addAttribute("cryptocurrency", new Cryptocurrency());

            return "index";
        }
        catch(Exception e){

            //Return the previously searched coin's price information if the currently searched coin cannot be found.
            found = false;

            model.addAttribute("line", line);
            model.addAttribute("list", timestamps);
            model.addAttribute("found", found);
            model.addAttribute("name", this.name);
            model.addAttribute("change_24h", change_24h);
            model.addAttribute("change_1h", change_1h);
            model.addAttribute("price", price);
            model.addAttribute("rank", rank);
            model.addAttribute("cryptocurrency", new Cryptocurrency());

            return "index";
        }
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}
