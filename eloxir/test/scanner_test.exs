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

  test "2-char tokens and single char ones that share a root" do
    source_code = """
    ! !=
    = ==
    < <=
    > >=
    """

    expected_tokens = [
      %Token{type: :bang, lexeme: "!", line: 1},
      %Token{type: :bang_equal, lexeme: "!=", line: 1},
      %Token{type: :equal, lexeme: "=", line: 2},
      %Token{type: :equal_equal, lexeme: "==", line: 2},
      %Token{type: :less, lexeme: "<", line: 3},
      %Token{type: :less_equal, lexeme: "<=", line: 3},
      %Token{type: :greater, lexeme: ">", line: 4},
      %Token{type: :greater_equal, lexeme: ">=", line: 4}
    ]

    run_test(source_code, expected_tokens)
  end

  defp run_test(source_code, expected_tokens) do
    {:ok, iodev} = StringIO.open(source_code)
    scanner = Scanner.new(iodev)
    tokens = collect_tokens(scanner)
    assert tokens == expected_tokens
  end

  defp collect_tokens(scanner, tokens \\ []) do
    token = Scanner.next_token(scanner)

    case token do
      :eof -> tokens
      _ -> collect_tokens(scanner, tokens ++ [token])
    end
  end

  # TODO identifiers, comments, slashes
end
