package com.example;

import io.sniffy.boot.EnableSniffy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableSniffy
@SpringBootApplication
public class SniffyStackoverflowApplication
{
	public static void main(String[] args) {
		SpringApplication.run(SniffyStackoverflowApplication.class, args);
	}
}
