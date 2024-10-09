defmodule ScannerTest do
  use ExUnit.Case, async: true
  doctest Scanner

  test "unambiguous single character tokens and line tracking" do
    source_code = """
    ()
    []
    {},.-+;*
    """

    expected_tokens = [
      %Token{type: :left_paren, lexeme: "(", line: 1},
      %Token{type: :right_paren, lexeme: ")", line: 1},
      %Token{type: :left_bracket, lexeme: "[", line: 2},
      %Token{type: :right_bracket, lexeme: "]", line: 2},
      %Token{type: :left_brace, lexeme: "{", line: 3},
      %Token{type: :right_brace, lexeme: "}", line: 3},
      %Token{type: :comma, lexeme: ",", line: 3},
      %Token{type: :dot, lexeme: ".", line: 3},
      %Token{type: :minus, lexeme: "-", line: 3},
      %Token{type: :plus, lexeme: "+", line: 3},
      %Token{type: :semicolon, lexeme: ";", line: 3},
      %Token{type: :star, lexeme: "*", line: 3}
    ]

    run_test(source_code, expected_tokens)
  end

  defp run_test(source_code, expected_tokens) do
    {:ok, pid} = StringIO.open(source_code)

    stream = IO.stream(pid, 1)
    tokens = Scanner.scan(stream)

    assert tokens == expected_tokens
  end
end
