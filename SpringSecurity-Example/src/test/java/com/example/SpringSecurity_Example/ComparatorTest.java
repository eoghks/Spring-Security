package com.example.SpringSecurity_Example;

import java.util.PriorityQueue;

class Student {
	int total;
	int mathScore;

	public Student(int total, int mathScore) {
		this.total = total;
		this.mathScore = mathScore;
	}
}

public class ComparatorTest {
	 public static void main(String[] args) {
		PriorityQueue<Student> pQ = new PriorityQueue<>((s1,s2)-> s2.total-s1.total);

		pQ.offer(new Student(70, 50));  // 우선순위 큐에 클래스 객체를 추가
        pQ.offer(new Student(60, 50));  // 우선순위 큐에 클래스 객체를 추가
        pQ.offer(new Student(65, 40));  // 우선순위 큐에 클래스 객체를 추가

        while (!pQ.isEmpty()) {
            Student s = pQ.poll();
            System.out.printf("Student\'s total and mathScore: %d, %d \n", s.total, s.mathScore);
        }
	 }
}
