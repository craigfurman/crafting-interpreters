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

  test "slashes and comments" do
    source_code = """
    !/! // comment
    !
    """

    expected_tokens = [
      %Token{type: :bang, lexeme: "!", line: 1},
      %Token{type: :slash, lexeme: "/", line: 1},
      %Token{type: :bang, lexeme: "!", line: 1},
      %Token{type: :bang, lexeme: "!", line: 2}
    ]

    run_test(source_code, expected_tokens)
  end

  test "identifiers and strings" do
    source_code = """
    var foo_09 = "bar";
    """

    expected_tokens = [
      %Token{type: :var, lexeme: "var", line: 1},
      %Token{type: :identifier, lexeme: "foo_09", literal: "foo_09", line: 1},
      %Token{type: :equal, lexeme: "=", line: 1},
      %Token{type: :string, lexeme: "\"bar\"", literal: "bar", line: 1},
      %Token{type: :semicolon, lexeme: ";", line: 1}
    ]

    run_test(source_code, expected_tokens)
  end

  test "short strings" do
    source_code = """
    ""
    "a"
    """

    expected_tokens = [
      %Token{type: :string, lexeme: "\"\"", literal: "", line: 1},
      %Token{type: :string, lexeme: "\"a\"", literal: "a", line: 2}
    ]

    run_test(source_code, expected_tokens)
  end

  test "numbers" do
    source_code = """
    123
    456.789
    """

    expected_tokens = [
      %Token{type: :number, lexeme: "123", literal: 123.0, line: 1},
      %Token{type: :number, lexeme: "456.789", literal: 456.789, line: 2}
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
end
